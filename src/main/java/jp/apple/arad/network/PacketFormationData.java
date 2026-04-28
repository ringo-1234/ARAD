package jp.apple.arad.network;

import io.netty.buffer.ByteBuf;
import jp.apple.arad.data.FormationSnapshot;
import jp.apple.arad.data.MapData;
import jp.apple.arad.data.PlayerSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class PacketFormationData implements IMessage {

    private List<FormationSnapshot> formations;
    private List<PlayerSnapshot> players;

    public PacketFormationData() {
    }

    public PacketFormationData(List<FormationSnapshot> formations, List<PlayerSnapshot> players) {
        this.formations = formations;
        this.players = players;
    }

    private static void writeString(ByteBuf buf, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(bytes.length, 255);
        buf.writeByte(len);
        buf.writeBytes(bytes, 0, len);
    }

    private static String readString(ByteBuf buf) {
        int len = buf.readByte() & 0xFF;
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeShort(formations.size());
        for (FormationSnapshot s : formations) {
            writeString(buf, s.id);
            buf.writeFloat(s.speed);
            buf.writeByte(s.notch);
            buf.writeByte(s.cars.size());
            for (float[] car : s.cars) {
                buf.writeFloat(car[0]);
                buf.writeFloat(car[1]);
                buf.writeFloat(car[2]);
            }
        }

        buf.writeShort(players.size());
        for (PlayerSnapshot p : players) {
            buf.writeFloat(p.x);
            buf.writeFloat(p.z);
            buf.writeFloat(p.yaw);
            writeString(buf, p.name);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int fCount = buf.readShort();
        formations = new ArrayList<>(fCount);
        for (int i = 0; i < fCount; i++) {
            String id = readString(buf);
            float speed = buf.readFloat();
            int notch = buf.readByte();
            int carCount = buf.readByte() & 0xFF;
            List<float[]> cars = new ArrayList<>(carCount);
            for (int c = 0; c < carCount; c++) {
                cars.add(new float[]{buf.readFloat(), buf.readFloat(), buf.readFloat()});
            }
            formations.add(new FormationSnapshot(id, speed, notch, cars));
        }

        int pCount = buf.readShort();
        players = new ArrayList<>(pCount);
        for (int i = 0; i < pCount; i++) {
            float x = buf.readFloat();
            float z = buf.readFloat();
            float yaw = buf.readFloat();
            String name = readString(buf);
            players.add(new PlayerSnapshot(x, z, yaw, name));
        }
    }

    public static final class Handler implements IMessageHandler<PacketFormationData, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketFormationData msg, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                MapData.INSTANCE.onFormationDataReceived(msg.formations);
                MapData.INSTANCE.onPlayerDataReceived(msg.players);
            });
            return null;
        }
    }
}