package net.createmod.ponder.client.guitutorial;

import java.lang.reflect.Field;
import java.util.*;
import net.createmod.ponder.api.gui.*;
import net.createmod.ponder.api.gui.GuiTutorial.*;
import net.createmod.ponder.api.gui.GuiTutorial.Stack;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/** Vanilla/Forge containers, numeric button IDs and GUI-relative authored regions. */
public final class StandardGuiAdapter implements GuiTutorialAdapter {
    @Override public boolean matches(GuiScreen gui,GuiTutorial t){
        if(t.screen().isEmpty())return false;
        for(Class<?> type=gui.getClass();type!=null;type=type.getSuperclass())if(type.getName().equals(t.screen()))return true;
        return false;
    }
    @Override public Rect window(GuiScreen gui,GuiTutorial t){
        if(gui instanceof GuiContainer c)return new Rect(number(c,"guiLeft","field_147003_i"),number(c,"guiTop","field_147009_r"),number(c,"xSize","field_146999_f"),number(c,"ySize","field_147000_g"));
        return new Rect((gui.width-t.width())/2,(gui.height-t.height())/2,t.width(),t.height());
    }
    private static int number(GuiContainer c,String mcp,String srg){return ReflectionHelper.<Integer,GuiContainer>getPrivateValue(GuiContainer.class,c,mcp,srg);}
    @SuppressWarnings("unchecked") public static List<GuiButton> buttons(GuiScreen gui){return ReflectionHelper.getPrivateValue(GuiScreen.class,gui,"buttonList","field_146292_n");}
    private Slot slot(GuiScreen gui,Target target){
        if(!(gui instanceof GuiContainer c)||target.type()!=TargetType.SLOT)return null;
        return c.inventorySlots.inventorySlots.stream().filter(s->s.slotNumber==target.index()).findFirst().orElse(null);
    }
    @Override public Rect target(GuiScreen gui,GuiTutorial tutorial,String key){
        Target t=tutorial.targets().get(key);if(t==null)return null;Rect window=window(gui,tutorial);
        if(t.type()==TargetType.SLOT){Slot s=slot(gui,t);return s!=null&&s.isEnabled()?new Rect(window.x()+s.xPos,window.y()+s.yPos,16,16):null;}
        if(t.type()==TargetType.BUTTON)return buttons(gui).stream().filter(b->b.id==t.index()&&b.visible).findFirst().map(b->new Rect(b.x,b.y,b.width,b.height)).orElse(null);
        return t.bounds().move(window.x(),window.y());
    }
    @Override public boolean enabled(GuiScreen gui,GuiTutorial tutorial,String key){
        Target t=tutorial.targets().get(key);
        return t!=null&&(t.type()!=TargetType.BUTTON||buttons(gui).stream().anyMatch(b->b.id==t.index()&&b.enabled&&b.visible));
    }
    @Override public Stack slot(GuiScreen gui,GuiTutorial tutorial,String key){
        Target t=tutorial.targets().get(key);Slot s=t==null?null:slot(gui,t);if(s==null||!s.isEnabled())return null;
        ItemStack stack=s.getStack();return stack.isEmpty()?Stack.EMPTY:new Stack(stack.getItem().getRegistryName().toString(),stack.getCount());
    }
    @Override public String value(GuiScreen screen,String key){
        if(screen instanceof GuiContainer c&&!c.inventorySlots.inventorySlots.isEmpty()&&key.startsWith("inventory_field.")){
            try {int index=Integer.parseInt(key.substring("inventory_field.".length()));var inventory=c.inventorySlots.getSlot(0).inventory;
                if(index>=0&&index<inventory.getFieldCount())return Integer.toString(inventory.getField(index));
            }catch(RuntimeException ignored){}
        }
        return "";
    }
    @Override public boolean textFocused(GuiScreen screen){
        for(Class<?> type=screen.getClass();type!=null&&GuiScreen.class.isAssignableFrom(type);type=type.getSuperclass())for(Field field:type.getDeclaredFields()){
            if(java.lang.reflect.Modifier.isStatic(field.getModifiers()))continue;
            try{field.setAccessible(true);Object value=field.get(screen);
                if(value instanceof GuiTextField text&&text.isFocused())return true;
                if(value instanceof Iterable<?> list){int n=0;for(Object entry:list){if(entry instanceof GuiTextField text&&text.isFocused())return true;if(++n>256)break;}}
            }catch(ReflectiveOperationException|RuntimeException ignored){}
        }
        return false;
    }
}
