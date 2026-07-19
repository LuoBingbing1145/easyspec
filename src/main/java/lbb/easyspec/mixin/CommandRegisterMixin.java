package lbb.easyspec.mixin;

import com.mojang.brigadier.CommandDispatcher;
import lbb.easyspec.command.EasySpecCommand;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Registers EasySpec commands into the server's command dispatcher
 * when the Commands class is initialized (at server start).
 */
@Mixin(Commands.class)
public class CommandRegisterMixin {

    @Shadow
    @Final
    private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onRegisterCommands(Commands.CommandSelection commandSelection, CommandBuildContext commandBuildContext, CallbackInfo ci) {
        EasySpecCommand.register(this.dispatcher);
    }
}
