/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package fredboat.command.music.control;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.Config;
import fredboat.FredBoat;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.audio.player.VideoSelection;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.I18n;
import fredboat.messaging.CentralMessaging;
import fredboat.perms.PermissionLevel;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;

public class SelectCommand extends Command implements IMusicCommand, ICommandRestricted {

    @Override
    public void onInvoke(CommandContext context) {
        select(context);
    }

    static void select(CommandContext context) {
        String[] args = context.args;
        Member invoker = context.invoker;
        GuildPlayer player = PlayerRegistry.get(context.guild);
        player.setCurrentTC(context.channel);
        VideoSelection selection = VideoSelection.get(invoker);
        if (selection != null) {
            try {
                int i = 1;

                if (args.length >= 1) {
                    String contentWithoutPrefix = args[0].substring(Config.CONFIG.getPrefix().length());
                    if (StringUtils.isNumeric(contentWithoutPrefix)) {
                        i = Integer.valueOf(contentWithoutPrefix);
                    } else {
                        i = Integer.valueOf(args[1]);
                    }
                }

                if (selection.choices.size() < i || i < 1) {
                    throw new NumberFormatException();
                } else {
                    AudioTrack selected = selection.choices.get(i - 1);
                    VideoSelection.remove(invoker);
                    TextChannel tc = FredBoat.getTextChannelById(Long.toString(selection.channelId));
                    if (tc != null) {
                        String msg = MessageFormat.format(I18n.get(context, "selectSuccess"), i, selected.getInfo().title, TextUtils.formatTime(selected.getInfo().length));
                        CentralMessaging.editMessage(tc, selection.outMsgId, CentralMessaging.from(msg));
                    }
                    player.queue(new AudioTrackContext(selected, invoker));
                    player.setPause(false);
                    context.deleteMessage();
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                context.reply(MessageFormat.format(I18n.get(context, "selectInterval"), selection.choices.size()));
            }
        } else {
            context.reply(I18n.get(context, "selectSelectionNotGiven"));
        }
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1} n OR {0}{2} n\n#";
        return usage + I18n.get(guild).getString("helpSelectCommand");
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.USER;
    }
}
