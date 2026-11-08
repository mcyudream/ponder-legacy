package net.createmod.ponder.gui;

import com.google.gson.*;
import java.util.*;
import net.createmod.ponder.api.gui.GuiTutorial;
import net.createmod.ponder.api.gui.GuiTutorial.*;
import net.createmod.ponder.guide.MarkdownTutorialCompiler;
import net.minecraft.util.ResourceLocation;

/** Bounded, strict data format shared by the editor, Markdown and Java integrations. */
public final class GuiTutorialJson {
    public static final int MAX_CHARACTERS=256*1024;
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private GuiTutorialJson() {}
    public static GuiTutorial parse(String source) {
        if(source.length()>MAX_CHARACTERS)throw bad("GUI tutorial exceeds 256 KiB characters");
        JsonObject root=new JsonParser().parse(source).getAsJsonObject();
        fields(root,"format","id","title","components","screen","adapter","width","height","texture","targets","steps");
        if(integer(root,"format",1,1,1)!=1)throw bad("Unsupported GUI tutorial format");
        ResourceLocation id=MarkdownTutorialCompiler.id(string(root,"id",""));
        String screen=string(root,"screen","");
        if(!screen.isEmpty()&&!screen.matches("[a-zA-Z_$][a-zA-Z0-9_$.]{0,255}"))throw bad("Invalid screen class name");
        int width=integer(root,"width",176,32,1024),height=integer(root,"height",166,32,1024);
        String texture=string(root,"texture","");if(!texture.isEmpty())MarkdownTutorialCompiler.id(texture);
        List<ResourceLocation> components=new ArrayList<>();
        if(root.has("components"))for(JsonElement c:root.getAsJsonArray("components")){
            components.add(MarkdownTutorialCompiler.id(c.getAsString()));if(components.size()>128)throw bad("Too many components");
        }
        Map<String,Target> targets=new LinkedHashMap<>();
        JsonObject ts=root.getAsJsonObject("targets");if(ts==null||ts.size()==0||ts.size()>256)throw bad("Use 1–256 GUI targets");
        for(var entry:ts.entrySet()){
            String key=entry.getKey();if(!key.matches("[a-zA-Z0-9_.-]{1,80}"))throw bad("Invalid target key: "+key);
            JsonObject t=entry.getValue().getAsJsonObject();fields(t,"type","index","x","y","width","height","label","item","count");
            TargetType type=enumeration(TargetType.class,string(t,"type","region"));
            Rect rect=new Rect(integer(t,"x",0,-1024,2048),integer(t,"y",0,-1024,2048),integer(t,"width",16,1,1024),integer(t,"height",16,1,1024));
            String item=item(t,"item");
            targets.put(key,new Target(type,integer(t,"index",0,0,4096),rect,string(t,"label",key),item,integer(t,"count",item.isEmpty()?0:1,0,4096)));
        }
        JsonArray ss=root.getAsJsonArray("steps");if(ss==null||ss.size()==0||ss.size()>256)throw bad("Use 1–256 GUI steps");
        List<Step> steps=new ArrayList<>();
        for(JsonElement element:ss){
            JsonObject s=element.getAsJsonObject();fields(s,"title","text","target","wait","duration","demo");
            String target=string(s,"target","");requireTarget(target,targets);
            int duration=integer(s,"duration",80,20,2400);
            JsonObject w=s.has("wait")?s.getAsJsonObject("wait"):new JsonObject();fields(w,"type","target","item","count","field","value");
            Wait type=enumeration(Wait.class,string(w,"type","next"));String wt=string(w,"target",target);requireTarget(wt,targets);
            if((type==Wait.SLOT_HAS||type==Wait.SLOT_EMPTY)&&targets.get(wt).type()!=TargetType.SLOT)throw bad("Slot conditions require a slot target");
            String field=string(w,"field",""),value=string(w,"value","");
            if((type==Wait.VALUE_EQUALS||type==Wait.VALUE_AT_LEAST)&&field.isBlank())throw bad("A value condition needs field");
            if(type==Wait.VALUE_AT_LEAST)number(value);
            Condition condition=new Condition(type,wt,item(w,"item"),integer(w,"count",1,1,4096),field,value);
            List<Action> actions=new ArrayList<>();
            if(s.has("demo"))for(JsonElement a:s.getAsJsonArray("demo")){
                if(actions.size()>=256)throw bad("Too many demo actions");
                JsonObject o=a.getAsJsonObject();fields(o,"op","target","item","count","field","value","at");
                Operation op=enumeration(Operation.class,string(o,"op",""));String atTarget=string(o,"target",target);
                if(op!=Operation.SET_VALUE)requireTarget(atTarget,targets);
                if(op==Operation.SET_SLOT&&targets.get(atTarget).type()!=TargetType.SLOT)throw bad("set_slot requires a slot target");
                String af=string(o,"field","");if(op==Operation.SET_VALUE&&af.isBlank())throw bad("set_value needs field");
                String ai=item(o,"item");
                actions.add(new Action(op,atTarget,ai,integer(o,"count",ai.isEmpty()?0:1,0,4096),af,string(o,"value",""),integer(o,"at",20,0,duration)));
            }
            actions.sort(Comparator.comparingInt(Action::at));
            steps.add(new Step(string(s,"title",""),string(s,"text",""),target,condition,duration,actions));
        }
        return new GuiTutorial(id,string(root,"title",id.getPath()),components,screen,string(root,"adapter","generic"),width,height,texture,targets,steps);
    }
    public static String write(GuiTutorial tutorial){return GSON.toJson(tree(tutorial));}
    public static JsonObject tree(GuiTutorial t){
        JsonObject o=new JsonObject();o.addProperty("format",1);o.addProperty("id",t.id().toString());o.addProperty("title",t.title());
        JsonArray cs=new JsonArray();t.components().forEach(c->cs.add(c.toString()));o.add("components",cs);
        o.addProperty("screen",t.screen());o.addProperty("adapter",t.adapter());o.addProperty("width",t.width());o.addProperty("height",t.height());o.addProperty("texture",t.texture());
        JsonObject targets=new JsonObject();t.targets().forEach((key,v)->{
            JsonObject a=new JsonObject();a.addProperty("type",v.type().name().toLowerCase(Locale.ROOT));a.addProperty("index",v.index());
            a.addProperty("x",v.bounds().x());a.addProperty("y",v.bounds().y());a.addProperty("width",v.bounds().width());a.addProperty("height",v.bounds().height());
            a.addProperty("label",v.label());a.addProperty("item",v.item());a.addProperty("count",v.count());targets.add(key,a);
        });o.add("targets",targets);JsonArray steps=new JsonArray();
        for(Step s:t.steps()){
            JsonObject a=new JsonObject();a.addProperty("title",s.title());a.addProperty("text",s.text());a.addProperty("target",s.target());a.addProperty("duration",s.duration());
            JsonObject w=new JsonObject();w.addProperty("type",s.condition().type().name().toLowerCase(Locale.ROOT));w.addProperty("target",s.condition().target());w.addProperty("item",s.condition().item());w.addProperty("count",s.condition().count());w.addProperty("field",s.condition().field());w.addProperty("value",s.condition().value());a.add("wait",w);
            JsonArray actions=new JsonArray();for(Action d:s.demo()){
                JsonObject b=new JsonObject();b.addProperty("op",d.op().name().toLowerCase(Locale.ROOT));b.addProperty("target",d.target());b.addProperty("item",d.item());b.addProperty("count",d.count());b.addProperty("field",d.field());b.addProperty("value",d.value());b.addProperty("at",d.at());actions.add(b);
            }a.add("demo",actions);steps.add(a);
        }o.add("steps",steps);return o;
    }
    private static void fields(JsonObject object,String... accepted){Set<String> keys=Set.of(accepted);for(var e:object.entrySet())if(!keys.contains(e.getKey()))throw bad("Unknown GUI field: "+e.getKey());}
    private static String string(JsonObject o,String key,String fallback){String s=o.has(key)?o.get(key).getAsString():fallback;if(s.length()>16384||s.indexOf('\0')>=0)throw bad("Invalid "+key);return s;}
    private static String item(JsonObject o,String key){String s=string(o,key,"");if(!s.isEmpty())MarkdownTutorialCompiler.id(s);return s;}
    private static int integer(JsonObject o,String key,int fallback,int min,int max){
        if(!o.has(key))return fallback;double d=o.get(key).getAsDouble();if(!Double.isFinite(d)||d!=Math.rint(d)||d<min||d>max)throw bad("Invalid "+key+" ("+min+"–"+max+")");return (int)d;
    }
    private static <T extends Enum<T>> T enumeration(Class<T> type,String value){try{return Enum.valueOf(type,value.toUpperCase(Locale.ROOT));}catch(IllegalArgumentException e){throw bad("Unknown "+type.getSimpleName()+": "+value);}}
    private static void requireTarget(String target,Map<String,Target> targets){if(!targets.containsKey(target))throw bad("Unknown GUI target: "+target);}
    public static double number(String value){try{double n=Double.parseDouble(value);if(Double.isFinite(n))return n;}catch(NumberFormatException ignored){}throw bad("Expected a finite number: "+value);}
    private static IllegalArgumentException bad(String message){return new IllegalArgumentException(message);}
}
