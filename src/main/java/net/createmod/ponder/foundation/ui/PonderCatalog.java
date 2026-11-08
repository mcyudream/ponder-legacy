package net.createmod.ponder.foundation.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.api.registration.TagRegistryAccess;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** A catalog snapshot: a component appears once, with the themes of all its scenes. */
final class PonderCatalog {
    final List<ItemEntry> items = new ArrayList<>();
    final Map<ResourceLocation, PonderTag> themes = new LinkedHashMap<>();

    static PonderCatalog current() {
        PonderCatalog catalog = new PonderCatalog(PonderIndex.getSceneAccess().getRegisteredEntries(), PonderIndex.getTagAccess(), id -> {
            ItemStack stack = PonderIndexScreen.stackFor(id);
            return !stack.isEmpty() && !PonderIndex.getIndexExclusions().isExcluded(stack.getItem());
        });
        for (var page : PonderIndex.getTutorials().all()) for (ResourceLocation id : page.components()) {
            ItemStack stack = PonderIndexScreen.stackFor(id);
            if (stack.isEmpty() || PonderIndex.getIndexExclusions().isExcluded(stack.getItem())) continue;
            ItemEntry item = catalog.items.stream().filter(i -> i.id.equals(id)).findFirst().orElse(null);
            if (item == null) { item = new ItemEntry(id); catalog.items.add(item); }
            item.tags.addAll(page.tags());
            if (page.scenes().isEmpty() && page.animation() == null) item.sceneCount++;
        }
        for (var tutorial : net.createmod.ponder.client.guitutorial.GuiTutorials.all()) for (ResourceLocation id : tutorial.components()) {
            ItemStack stack=PonderIndexScreen.stackFor(id);
            if (stack.isEmpty() || PonderIndex.getIndexExclusions().isExcluded(stack.getItem())) continue;
            ItemEntry item=catalog.items.stream().filter(i->i.id.equals(id)).findFirst().orElse(null);
            if (item==null) { item=new ItemEntry(id); catalog.items.add(item); }
            item.sceneCount++;
        }
        return catalog;
    }

    PonderCatalog(Collection<Map.Entry<ResourceLocation, StoryBoardEntry>> scenes, TagRegistryAccess tags,
                  Predicate<ResourceLocation> visible) {
        for (PonderTag theme : tags.getListedTags()) themes.put(theme.getId(), theme);
        Map<ResourceLocation, ItemEntry> byComponent = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, StoryBoardEntry> scene : scenes) {
            ResourceLocation id = scene.getKey();
            if (!byComponent.containsKey(id)) {
                if (!visible.test(id)) continue;
                ItemEntry item = new ItemEntry(id);
                for (PonderTag tag : tags.getTags(id)) item.tags.add(tag.getId());
                byComponent.put(id, item);
            }
            ItemEntry item = byComponent.get(id);
            item.sceneCount++;
            item.tags.addAll(scene.getValue().getTags());
            item.tags.remove(PonderTag.Highlight.ALL);
        }
        items.addAll(byComponent.values());
    }

    List<ItemEntry> filter(String namespace, ResourceLocation tag, boolean uncategorized, String query,
                           Function<ResourceLocation, String> itemName) {
        String search = query.trim().toLowerCase(Locale.ROOT);
        List<ItemEntry> result = new ArrayList<>();
        for (ItemEntry item : items) {
            if (namespace != null && !item.id.getNamespace().equals(namespace)) continue;
            if (tag != null && !item.tags.contains(tag)) continue;
            if (uncategorized && item.tags.stream().anyMatch(themes::containsKey)) continue;
            if (!search.isEmpty() && !(item.id + "\n" + itemName.apply(item.id)).toLowerCase(Locale.ROOT).contains(search)) continue;
            result.add(item);
        }
        return result;
    }

    static final class ItemEntry {
        final ResourceLocation id;
        final Set<ResourceLocation> tags = new LinkedHashSet<>();
        int sceneCount;
        ItemEntry(ResourceLocation id) { this.id = id; }
    }
}
