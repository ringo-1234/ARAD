package jp.apple.arad.cache;

import jp.apple.arad.AradCore;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class RailCacheManager {

    public static final RailCacheManager INSTANCE = new RailCacheManager();

    private final Map<String, List<CachedRail>> cache = new HashMap<>();

    private String currentServerId = null;

    private RailCacheManager() {}

    public void onWorldJoin(String serverId) {
        this.currentServerId = serverId;
        cache.clear();
        loadFromDisk(serverId);
    }

    public void onWorldLeave() {
        if (currentServerId != null) {
            saveToDisk(currentServerId);
        }
        currentServerId = null;
        cache.clear();
    }

    public void updateChunk(String chunkKey, List<CachedRail> segments) {
        if (segments.isEmpty()) {
            cache.remove(chunkKey);
        } else {
            cache.put(chunkKey, Collections.unmodifiableList(new ArrayList<>(segments)));
        }
    }

    public List<CachedRail> getAllSegments() {
        List<CachedRail> all = new ArrayList<>();
        for (List<CachedRail> segs : cache.values()) {
            all.addAll(segs);
        }
        return all;
    }

    public int cachedChunkCount() {
        return cache.size();
    }

    public static String makeChunkKey(int dim, int chunkX, int chunkZ) {
        return dim + ":" + chunkX + ":" + chunkZ;
    }

    private void saveToDisk(String serverId) {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagList chunkList = new NBTTagList();

        for (Map.Entry<String, List<CachedRail>> e : cache.entrySet()) {
            NBTTagCompound chunkTag = new NBTTagCompound();
            chunkTag.setString("key", e.getKey());

            NBTTagList segList = new NBTTagList();
            for (CachedRail seg : e.getValue()) {
                segList.appendTag(seg.toNBT());
            }
            chunkTag.setTag("segs", segList);
            chunkList.appendTag(chunkTag);
        }

        root.setTag("chunks", chunkList);

        try {
            CompressedStreamTools.write(root, getCacheFile(serverId));
            AradCore.LOGGER.info("[Arad] Cache saved — {} chunks", cache.size());
        } catch (IOException ex) {
            AradCore.LOGGER.warn("[Arad] Failed to save cache", ex);
        }
    }

    private void loadFromDisk(String serverId) {
        File file = getCacheFile(serverId);
        if (!file.exists()) return;

        try {
            NBTTagCompound root = CompressedStreamTools.read(file);
            NBTTagList chunkList = root.getTagList("chunks", 10);

            for (int i = 0; i < chunkList.tagCount(); i++) {
                NBTTagCompound chunkTag = chunkList.getCompoundTagAt(i);
                String key = chunkTag.getString("key");
                NBTTagList segList = chunkTag.getTagList("segs", 10);

                List<CachedRail> segs = new ArrayList<>(segList.tagCount());
                for (int j = 0; j < segList.tagCount(); j++) {
                    segs.add(CachedRail.fromNBT(segList.getCompoundTagAt(j)));
                }
                cache.put(key, Collections.unmodifiableList(segs));
            }
        } catch (IOException ex) {
            AradCore.LOGGER.warn("[Arad] Failed to load cache", ex);
        }
    }

    private File getCacheFile(String serverId) {
        File dir = new File(
                net.minecraft.client.Minecraft.getMinecraft().mcDataDir,
                "arad_cache"
        );
        if (!dir.exists()) dir.mkdirs();
        
        String safe = serverId.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return new File(dir, safe + ".nbt");
    }
}