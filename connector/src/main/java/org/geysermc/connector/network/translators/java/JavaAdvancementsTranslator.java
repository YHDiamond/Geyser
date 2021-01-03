/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.network.translators.java;

import com.github.steveice10.mc.protocol.data.game.advancement.Advancement;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerAdvancementsPacket;
import com.nukkitx.protocol.bedrock.packet.SetTitlePacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.chat.MessageTranslator;
import org.geysermc.connector.utils.AdvancementsUtils;
import org.geysermc.connector.utils.LocaleUtils;

import java.util.Map;

@Translator(packet = ServerAdvancementsPacket.class)
public class JavaAdvancementsTranslator extends PacketTranslator<ServerAdvancementsPacket> {

    @Override
    public void translate(ServerAdvancementsPacket packet, GeyserSession session) {
        sendToolbarAdvancementUpdates(session, packet);
        // Removes removed advancements from player's stored advancements
        for (String removedAdvancement : packet.getRemovedAdvancements()) {
            session.getStoredAdvancements().remove(removedAdvancement);
        }

        session.setStoredAdvancementProgress(packet.getProgress());

        // Adds advancements to the player's stored advancements when advancements are sent
        // Also sends notifications for any new advancements
        for (Advancement advancement : packet.getAdvancements()) {
            if (advancement.getDisplayData() != null && !advancement.getDisplayData().isHidden()){
                session.getStoredAdvancements().put(advancement.getId(), advancement);
            } else {
                session.getStoredAdvancements().remove(advancement.getId());
            }
        }
    }

    // Handle all advancements progress updates
    public static void sendToolbarAdvancementUpdates(GeyserSession session, ServerAdvancementsPacket packet) {
        if (!session.getStoredAdvancementProgress().isEmpty()) {
            for (Map.Entry<String, Map<String, Long>> progress : packet.getProgress().entrySet()) {
                Advancement advancement = session.getStoredAdvancements().get(progress.getKey());
                if (advancement != null && advancement.getDisplayData() != null) {
                    String color = AdvancementsUtils.ADVANCEMENT_FRAME_TYPES_TO_COLOR_CODES.get(advancement.getDisplayData().getFrameType().toString());
                    String advancementName = MessageTranslator.convertMessage(advancement.getDisplayData().getTitle(), session.getLocale());
                    boolean earned = true;

                    for (Map.Entry<String, Long> entry : packet.getProgress().get(advancement.getId()).entrySet()) {
                        if (entry.getValue() == -1) {
                            earned = false;
                            break;
                        }
                    }

                    if (earned) {
                        SetTitlePacket titlePacket = new SetTitlePacket();
                        titlePacket.setText(color + "[" + LocaleUtils.getLocaleString("advancements.toast." + advancement.getDisplayData().getFrameType().toString().toLowerCase(), session.getLocale()) + "] " + advancementName);
                        titlePacket.setType(SetTitlePacket.Type.ACTIONBAR);
                        titlePacket.setFadeOutTime(3);
                        titlePacket.setFadeInTime(3);
                        titlePacket.setStayTime(3);
                        session.sendUpstreamPacket(titlePacket);
                    }
                } else {
                    session.getStoredAdvancements().remove(advancement.getId(), advancement);
                }
            }
        }
    }
}