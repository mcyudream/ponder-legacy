package net.createmod.ponder.gui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import net.createmod.ponder.api.gui.GuiTutorial;
import net.createmod.ponder.guide.MarkdownTutorialCompiler;
import net.minecraft.util.ResourceLocation;

/** Validated atomic local authoring; no file path from a tutorial may escape its root. */
public final class GuiTutorialFiles {
    private GuiTutorialFiles() {}
    public static Path path(Path root,ResourceLocation id)throws IOException{
        MarkdownTutorialCompiler.id(id.toString());root=root.toAbsolutePath().normalize();
        Path target=root.resolve(id.getNamespace()).resolve(id.getPath()+".gui.json").normalize();
        if(!target.startsWith(root))throw new IOException("GUI tutorial escaped its directory");
        for(Path p=target;p!=null&&p.startsWith(root);p=p.getParent())if(Files.isSymbolicLink(p))throw new IOException("GUI tutorial paths cannot contain symlinks");
        return target;
    }
    public static void save(Path root,GuiTutorial tutorial)throws IOException{
        String source=GuiTutorialJson.write(tutorial);GuiTutorialJson.parse(source);
        Path target=path(root,tutorial.id());Files.createDirectories(target.getParent());
        Path temp=Files.createTempFile(target.getParent(),".gui-",".tmp");
        try{
            Files.writeString(temp,source,StandardCharsets.UTF_8);
            try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temp);}
    }
    public static String read(Path path)throws IOException{
        if(Files.isSymbolicLink(path)||!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)||Files.size(path)>GuiTutorialJson.MAX_CHARACTERS*4L)throw new IOException("Invalid GUI tutorial file: "+path);
        try(var stream=Files.newInputStream(path)){
            byte[] bytes=stream.readNBytes(GuiTutorialJson.MAX_CHARACTERS*4+1);
            if(bytes.length>GuiTutorialJson.MAX_CHARACTERS*4)throw new IOException("GUI tutorial file too large");
            return new String(bytes,StandardCharsets.UTF_8);
        }
    }
}
