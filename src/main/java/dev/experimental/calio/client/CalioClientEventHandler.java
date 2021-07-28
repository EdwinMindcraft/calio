package dev.experimental.calio.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.experimental.calio.api.CalioAPI;
import dev.experimental.calio.common.network.CalioNetwork;
import dev.experimental.calio.common.network.packet.C2SShareItemPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = CalioAPI.MODID)
public class CalioClientEventHandler {
	private static boolean sharedStack = false;

	@SubscribeEvent
	public static void renderTick(TickEvent.RenderTickEvent event) {
		if (event.type == TickEvent.Type.CLIENT && event.phase == TickEvent.Phase.START) {
			Minecraft client = Minecraft.getInstance();
			long window = client.getWindow().getWindow();
			if (client.player != null && client.screen instanceof AbstractContainerScreen<?> screen) {
				Slot focusedSlot = screen.hoveredSlot;
				boolean isCtrlPressed = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL);
				InputConstants.Key key = client.options.keyChat.getKey();
				boolean isChatPressed = InputConstants.isKeyDown(window, key.getValue());
				if (isCtrlPressed && isChatPressed && !sharedStack) {
					sharedStack = true;
					if (client.player.containerMenu.getCarried().isEmpty() && focusedSlot != null && focusedSlot.hasItem()) {
						CalioNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(), new C2SShareItemPacket(focusedSlot.getItem()));
					}
				}
				if (sharedStack && (!isCtrlPressed || !isChatPressed)) {
					sharedStack = false;
				}
			}
		}
	}
}
