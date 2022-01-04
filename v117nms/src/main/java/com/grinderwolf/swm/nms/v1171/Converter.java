package com.grinderwolf.swm.nms.v1171;

import com.flowpowered.nbt.*;
import com.flowpowered.nbt.TagType;
import com.grinderwolf.swm.api.utils.*;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.chunk.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Converter {

    private static final Logger LOGGER = LogManager.getLogger("SWM Converter");

    static Tag convertTag(com.flowpowered.nbt.Tag<?> tag) {
        try {
            switch (tag.getType()) {
                case TAG_BYTE:
                    return ByteTag.valueOf(((com.flowpowered.nbt.ByteTag) tag).getValue());
                case TAG_SHORT:
                    return ShortTag.valueOf(((com.flowpowered.nbt.ShortTag) tag).getValue());
                case TAG_INT:
                    return IntTag.valueOf(((com.flowpowered.nbt.IntTag) tag).getValue());
                case TAG_LONG:
                    return LongTag.valueOf(((com.flowpowered.nbt.LongTag) tag).getValue());
                case TAG_FLOAT:
                    return FloatTag.valueOf(((com.flowpowered.nbt.FloatTag) tag).getValue());
                case TAG_DOUBLE:
                    return DoubleTag.valueOf(((com.flowpowered.nbt.DoubleTag) tag).getValue());
                case TAG_BYTE_ARRAY:
                    return new ByteArrayTag(((com.flowpowered.nbt.ByteArrayTag) tag).getValue());
                case TAG_STRING:
                    return StringTag.valueOf(((com.flowpowered.nbt.StringTag) tag).getValue());
                case TAG_LIST:
                    ListTag list = new ListTag();
                    for (com.flowpowered.nbt.Tag<?> value : ((com.flowpowered.nbt.ListTag<?>) tag).getValue()) {
                        list.add(convertTag(value));
                    }

                    return list;
                case TAG_COMPOUND:
                    CompoundTag compoundTag = new CompoundTag();
                    for (var entry : ((com.flowpowered.nbt.CompoundTag) tag).getValue().entrySet()) {
                        compoundTag.put(entry.getKey(), convertTag(entry.getValue()));
                    }

                    return compoundTag;
                case TAG_INT_ARRAY:
                    return new IntArrayTag(((com.flowpowered.nbt.IntArrayTag) tag).getValue());
                case TAG_LONG_ARRAY:
                    return new LongArrayTag(((com.flowpowered.nbt.LongArrayTag) tag).getValue());
                default:
                    throw new IllegalArgumentException("Invalid tag type " + tag.getType().name());
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to convert NBT object:");
            LOGGER.error(tag.toString());

            throw ex;
        }
    }

    static com.flowpowered.nbt.Tag<?> convertTag(String name, Tag base) {
        switch (base.getId()) {
            case 1:
                return new com.flowpowered.nbt.ByteTag(name, ((ByteTag) base).getAsByte());
            case 2:
                return new com.flowpowered.nbt.ShortTag(name, ((ShortTag) base).getAsShort());
            case 3:
                return new com.flowpowered.nbt.IntTag(name, ((IntTag) base).getAsInt());
            case 4:
                return new com.flowpowered.nbt.LongTag(name, ((LongTag) base).getAsLong());
            case 5:
                return new com.flowpowered.nbt.FloatTag(name, ((FloatTag) base).getAsFloat());
            case 6:
                return new com.flowpowered.nbt.DoubleTag(name, ((DoubleTag) base).getAsDouble());
            case 7:
                return new com.flowpowered.nbt.ByteArrayTag(name, ((ByteArrayTag) base).getAsByteArray());
            case 8:
                return new com.flowpowered.nbt.StringTag(name, ((StringTag) base).getAsString());
            case 9:
                List<com.flowpowered.nbt.Tag<?>> list = new ArrayList<>();
                ListTag originalList = ((ListTag) base);

                for (Tag entry : originalList) {
                    list.add(convertTag("", entry));
                }

                return new com.flowpowered.nbt.ListTag<>(name, TagType.getById(originalList.getElementType()), list);
            case 10:
                CompoundTag originalCompound = ((CompoundTag) base);
                com.flowpowered.nbt.CompoundTag compound = new com.flowpowered.nbt.CompoundTag(name, new CompoundMap());

                for (String key : originalCompound.getAllKeys()) {
                    compound.getValue().put(key, convertTag(key, originalCompound.get(key)));
                }

                return compound;
            case 11:
                return new com.flowpowered.nbt.IntArrayTag(name, ((IntArrayTag) base).getAsIntArray());
            case 12:
                return new com.flowpowered.nbt.LongArrayTag(name, ((LongArrayTag) base).getAsLongArray());
            default:
                throw new IllegalArgumentException("Invalid tag type " + base.getId());
        }
    }

    static DataLayer convertArray(NibbleArray array) {
        return new DataLayer(array.getBacking());
    }

    static NibbleArray convertArray(DataLayer array) {
        if (array == null) {
            return null;
        }

        return new NibbleArray(array.getDataRaw());
    }


}