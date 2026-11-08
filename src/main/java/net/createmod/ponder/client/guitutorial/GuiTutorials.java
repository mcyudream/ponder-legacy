package net.createmod.ponder.client.guitutorial;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.gui.*;
import net.createmod.ponder.api.guide.Tutorial;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.gui.*;
import net.createmod.ponder.guide.GuideSources;
import net.createmod.ponder.guide.MarkdownTutorialCompiler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;

/** Client-only registry. Resource/local lessons remain optional and reloadable. */
public final class GuiTutorials {
    private static final Map<String,GuiTutorialAdapter> ADAPTERS=new LinkedHashMap<>();
    private static final Map<ResourceLocation,GuiTutorial> API=new LinkedHashMap<>();
    private static final Map<String,GuiTutorial> LAST_GOOD=new LinkedHashMap<>();
    private static Map<ResourceLocation,GuiTutorial> lessons=Map.of();
    private static final List<String> errors=new ArrayList<>();
    private static Object cachedPages,cachedLessons;
    private static List<GuiTutorial> cachedAll=List.of();
    private static long revision;
    public static long revision(){return revision;}
    static{ADAPTERS.put("generic",new StandardGuiAdapter());}
    private GuiTutorials(){}
    public static Path root(){return GuideSources.root().getParent().resolve("gui");}
    public static void registerAdapter(String id,GuiTutorialAdapter adapter){ADAPTERS.put(Objects.requireNonNull(id),Objects.requireNonNull(adapter));}
    public static GuiTutorialAdapter adapter(GuiTutorial tutorial){return ADAPTERS.get(tutorial.adapter());}
    public static void register(GuiTutorial tutorial){API.put(tutorial.id(),GuiTutorialJson.parse(GuiTutorialJson.write(tutorial)));reload();}
    public static List<String> errors(){return List.copyOf(errors);}
    public static List<GuiTutorial> all(){
        if(cachedPages==PonderIndex.getTutorials()&&cachedLessons==lessons)return cachedAll;
        Map<ResourceLocation,GuiTutorial> result=new LinkedHashMap<>(lessons);
        for(var page:PonderIndex.getTutorials().all())for(var block:page.blocks())if(block.kind()==Tutorial.Kind.GUI_TUTORIAL){
            try{GuiTutorial tutorial=resolve(block.target());result.putIfAbsent(tutorial.id(),tutorial);}catch(RuntimeException ignored){}
        }
        cachedPages=PonderIndex.getTutorials();cachedLessons=lessons;cachedAll=List.copyOf(result.values());return cachedAll;
    }
    public static List<GuiTutorial> forComponent(ResourceLocation id){return all().stream().filter(t->t.components().contains(id)).toList();}
    public static List<GuiTutorial> forScreen(GuiScreen screen){
        if(screen==null)return List.of();
        return all().stream().filter(t->{GuiTutorialAdapter a=adapter(t);return a!=null&&a.matches(screen,t);}).toList();
    }
    public static GuiTutorial resolve(String source){
        if(source.stripLeading().startsWith("{"))return GuiTutorialJson.parse(source);
        ResourceLocation id=MarkdownTutorialCompiler.id(source);GuiTutorial t=lessons.get(id);
        return t!=null?t:resource(id);
    }
    public static void save(GuiTutorial tutorial)throws IOException{GuiTutorialFiles.save(root(),tutorial);reload();}
    public static GuiTutorial furnaceExample(){return resource(new ResourceLocation("ponder:examples/furnace"));}
    public static GuiTutorial resource(ResourceLocation id){
        var mc=Minecraft.getMinecraft();String lang=mc.getLanguageManager().getCurrentLanguage().getLanguageCode();
        for(String p:List.of("ponder/gui/"+lang+"/"+id.getPath()+".json","ponder/gui/en_us/"+id.getPath()+".json","ponder/gui/"+id.getPath()+".json")){
            try(var resource=mc.getResourceManager().getResource(new ResourceLocation(id.getNamespace(),p));var stream=resource.getInputStream()){
                GuiTutorial lesson=GuiTutorialJson.parse(bounded(stream));if(!lesson.id().equals(id))throw new IllegalArgumentException("GUI resource id mismatch: "+id);return lesson;
            }catch(FileNotFoundException ignored){}catch(IOException e){throw new IllegalArgumentException("Cannot read GUI resource "+id,e);}
        }
        throw new IllegalArgumentException("Missing GUI tutorial: "+id);
    }
    public static void reload(){
        Map<ResourceLocation,GuiTutorial> next=new LinkedHashMap<>(API);errors.clear();
        PonderIndex.forEachPlugin(plugin->{try{plugin.registerGuiTutorials(t->next.put(t.id(),GuiTutorialJson.parse(GuiTutorialJson.write(t))));}catch(RuntimeException e){error(plugin.getModId()+": "+e.getMessage());}});
        var manager=Minecraft.getMinecraft().getResourceManager();
        for(String domain:manager.getResourceDomains()){
            try(var index=manager.getResource(new ResourceLocation(domain,"ponder/gui/index.json"));var stream=index.getInputStream()){
                JsonArray entries=new JsonParser().parse(bounded(stream)).getAsJsonObject().getAsJsonArray("tutorials");
                if(entries.size()>1024)throw new IOException("Too many GUI tutorials");
                for(JsonElement entry:entries){String id=entry.getAsString();GuiTutorial t=resource(MarkdownTutorialCompiler.id(id.contains(":")?id:domain+":"+id));next.put(t.id(),t);}
            }catch(FileNotFoundException ignored){}catch(IOException|RuntimeException e){error(domain+": "+e.getMessage());}
        }
        Map<String,GuiTutorial> good=new LinkedHashMap<>();
        if(Files.isDirectory(root()))try(var paths=Files.walk(root())){
            List<Path> files=paths.filter(p->p.toString().endsWith(".gui.json")).sorted().toList();if(files.size()>1024)throw new IOException("Too many GUI tutorial files");
            long total=0;for(Path path:files){String key=root().relativize(path).toString();try{
                if((total+=Files.size(path))>8*1024*1024)throw new IOException("GUI tutorial files exceed 8 MiB");
                // Validate every parent, including symlinks to directories outside the workspace.
                for(Path p=path;p!=null&&p.startsWith(root());p=p.getParent())if(Files.isSymbolicLink(p))throw new IOException("Symlink in GUI path");
                GuiTutorial tutorial=GuiTutorialJson.parse(GuiTutorialFiles.read(path));good.put(key,tutorial);
            }catch(IOException|RuntimeException e){error(key+": "+e.getMessage());if(LAST_GOOD.containsKey(key))good.put(key,LAST_GOOD.get(key));}}
        }catch(IOException e){error(e.getMessage());}
        LAST_GOOD.clear();LAST_GOOD.putAll(good);good.values().forEach(t->next.put(t.id(),t));lessons=Collections.unmodifiableMap(next);revision++;
    }
    private static String bounded(InputStream stream)throws IOException{byte[] b=stream.readNBytes(GuiTutorialJson.MAX_CHARACTERS*4+1);if(b.length>GuiTutorialJson.MAX_CHARACTERS*4)throw new IOException("GUI resource too large");return new String(b,StandardCharsets.UTF_8);}
    private static void error(String text){errors.add(text);Ponder.LOGGER.warn("GUI tutorial: {}",text);}
}
