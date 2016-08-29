package flavor.pie.countdown;

import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.filter.cause.Named;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.TransferResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.title.Title;
import org.spongepowered.api.util.ban.Ban;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Plugin(id="countdown",name="Countdown",version="1.0.0",authors="pie_flavor",description="A time-based currency, similar to that of the movie In Time.")
public class Countdown {
    @Inject
    Game game;
    @Inject
    Logger logger;
    @Inject @DefaultConfig(sharedRoot = true)
    ConfigurationLoader<CommentedConfigurationNode> loader;
    @Inject @DefaultConfig(sharedRoot = true)
    Path configPath;
    ConfigurationNode root;
    CountdownEconomyService svc;
    CountdownData.Builder builder;
    boolean ban;
    long banTicks;
    boolean spend;

    @Listener
    public void preInit(GamePreInitializationEvent e) {
        Asset asset = game.getAssetManager().getAsset(this, "default.conf").get();
        if (!Files.exists(configPath)) {
            try {
                asset.copyToFile(configPath);
                root = loader.load();
            } catch (IOException ex) {
                logger.error("Could not save default config! Using default.");
                ex.printStackTrace();
                loader = HoconConfigurationLoader.builder().setURL(asset.getUrl()).build();
                try {
                    root = loader.load();
                } catch (IOException ex2) {
                    logger.error("Could not load config! (From a JAR resource? WHAT DID YOU DO?)");
                    ex2.printStackTrace();
                    disable();
                    return;
                }
            }
        } else {
            try {
                root = loader.load();
            } catch (IOException ex) {
                logger.error("Could not load config! Using the default.");
                ex.printStackTrace();
                loader = HoconConfigurationLoader.builder().setURL(asset.getUrl()).build();
                try {
                    root = loader.load();
                } catch (IOException ex2) {
                    logger.error("Could not load config! (From a JAR resource? WHAT DID YOU DO?)");
                    ex2.printStackTrace();
                    disable();
                    return;
                }
            }
        }
        long ticks = root.getNode("initial-ticks").getLong(24192000);
        long startTicks = root.getNode("ticks-until-start").getLong(-1);
        long banTicks = root.getNode("ban-ticks").getLong(-1);
        ban = root.getNode("ban").getBoolean();
        spend = root.getNode("spend-enabled").getBoolean();
        builder = new CountdownData.Builder(ticks, startTicks, this);
        game.getDataManager().register(CountdownData.class, CountdownData.Immutable.class, builder);
        CountdownRegistryModule module = new CountdownRegistryModule();
        game.getRegistry().registerModule(Currency.class, module);
        CountdownEconomyService svc = new CountdownEconomyService(ticks, this);
        this.svc = svc;
        game.getServiceManager().setProvider(this, EconomyService.class, svc);
    }
    @Listener
    public void init(GameInitializationEvent e) {
        CommandSpec bal = CommandSpec.builder()
                .description(Text.of("Gets how much time you have."))
                .executor(this::bal)
                .build();
        CommandSpec pay = CommandSpec.builder()
                .description(Text.of("Pays another person time."))
                .executor(this::pay)
                .arguments(GenericArguments.player(Text.of("to")), TemporalCurrencies.currencyDuration(Text.of("time")))
                .build();
        CommandSpec start = CommandSpec.builder()
                .description(Text.of("Starts someone's clock."))
                .executor(this::start)
                .arguments(GenericArguments.playerOrSource(Text.of("player")))
                .build();
        CommandSpec setStart = CommandSpec.builder()
                .description(Text.of("Sets the amount of time until someone starts."))
                .executor(this::setStart)
                .arguments(
                        GenericArguments.player(Text.of("player")),
                        TemporalCurrencies.currencyDuration(Text.of("time"))
                )
                .permission("countdown.setstart")
                .build();
        game.getCommandManager().register(this, bal, "balance", "bal", "mytime", "clock");
        game.getCommandManager().register(this, pay, "pay", "transfer");
        game.getCommandManager().register(this, start, "start", "clockstart", "cs");
        game.getCommandManager().register(this, setStart, "setstart", "setstarttime", "sst");
    }
    void disable() {
        game.getEventManager().unregisterPluginListeners(this);
    }
    @Listener
    public void started(GameStartedServerEvent e) {
        startTask();
    }
    void startTask() {
        Task.builder().delayTicks(1).execute(this::tick).intervalTicks(1).name("countdown-S-TickCounter");
    }
    void tick() {
        for (Player p : game.getServer().getOnlinePlayers()) {
            CountdownData data = p.getOrCreate(CountdownData.class).get();
            if (data.waitTicks < 0) continue;
            if (data.waitTicks == 0) {
                if (data.ticks == 0) {
                    p.offer(Keys.HEALTH, 0d, Cause.of(NamedCause.of("Plugin", this)));
                    if (ban) {
                        Ban.Builder builder = Ban.builder()
                                .profile(p.getProfile())
                                .reason(Text.of(TextColors.RED, "0000:00:00:00:00:00:00", Text.NEW_LINE, TextColors.DARK_RED, "You timed out!"))
                                .source((Text) null);
                        if (banTicks > 0) {
                            builder.expirationDate(Instant.now().plus(banTicks, TemporalCurrencies.TICKS));
                        }
                        game.getServiceManager().provideUnchecked(BanService.class).addBan(builder.build());
                    }
                } else {
                    data.ticks -= 1;
                }
            } else {
                data.waitTicks -= 1;
                if (data.waitTicks == 0) {
                    p.sendTitle(Title.builder()
                            .title(Text.of(TextColors.RED, "Your timer has started!"))
                            .fadeIn(20)
                            .stay(60)
                            .fadeOut(20)
                            .build());
                    p.sendMessage(ChatTypes.SYSTEM, Text.of(TextColors.RED, "Your timer has started!"));
                }
            }
            p.offer(data);
        }
    }
    @Listener
    public void destruct(DestructEntityEvent.Death e, @Named("Plugin") Countdown plugin) {
        e.setMessage(Text.of(e.getTargetEntity().get(Keys.DISPLAY_NAME).get(), " timed out (", TextColors.RED, "0000:00:00:00:00:00:00", TextColors.WHITE, ")"));
    }

    public CommandResult bal(CommandSource src, CommandContext args) throws CommandException {
        if (!(src instanceof Player)) throw new CommandException(Text.of("Only players can use this command!"));
        Player p = (Player) src;
        CountdownData data = p.getOrCreate(CountdownData.class).get();
        String balanceStr = TemporalCurrencies.CurrencyDuration.of(data.ticks).toString();
        Text balance = Text.of("Current balance: ", (data.waitTicks != 0 ? TextColors.RED : TextColors.GREEN), balanceStr);
        p.sendMessage(balance);
        return CommandResult.builder().queryResult((int) Math.max((long) Integer.MAX_VALUE, data.ticks)).build();
    }

    public CommandResult pay(CommandSource src, CommandContext args) throws CommandException {
        if (!(src instanceof Player)) throw new CommandException(Text.of("Only players can use this command!"));
        Player from = (Player) src;
        Player to = args.<Player>getOne("to").get();
        TemporalCurrencies.CurrencyDuration duration = args.<TemporalCurrencies.CurrencyDuration>getOne("time").get();
        long ticks = duration.getDuration().toMillis() / 50;
        Account fromAcc = svc.getOrCreateAccount(from.getUniqueId()).get();
        Account toAcc = svc.getOrCreateAccount(to.getUniqueId()).get();
        CountdownData fromData = from.getOrCreate(CountdownData.class).get();
        CountdownData toData = to.getOrCreate(CountdownData.class).get();
        if (!spend) {
            if (fromData.waitTicks > 0) {
                from.sendMessage(Text.of("Your timer hasn't started yet!"));
            }
            if (toData.waitTicks > 0) {
                from.sendMessage(Text.of(to.getName(), "'s timer hasn't started yet!"));
            }
        }
        TransferResult result = fromAcc.transfer(toAcc, TemporalCurrencies.Tick, BigDecimal.valueOf(ticks), Cause.of(NamedCause.source(from)));
        switch (result.getResult()) {
            case SUCCESS:
                from.sendMessage(Text.of("Successfully transferred ", duration.toString(), " to ", to.getName(), "."));
                from.sendMessage(Text.of("You now have ", (fromData.waitTicks > 0 ? TextColors.RED : TextColors.GREEN), TemporalCurrencies.CurrencyDuration.of(fromData.ticks)));
                to.sendMessage(Text.of(from.getName(), " has paid you ", duration.toString(), "."));
                to.sendMessage(Text.of("You now have ", (toData.waitTicks > 0 ? TextColors.RED : TextColors.GREEN), TemporalCurrencies.CurrencyDuration.of(toData.ticks)));
                return CommandResult.builder().queryResult((int) Math.max((long) Integer.MAX_VALUE, duration.getDuration().get(TemporalCurrencies.TICKS))).build();
            case ACCOUNT_NO_FUNDS:
                from.sendMessage(Text.of("You don't have enough time! (Tread carefully so you don't time yourself out!)"));
                return CommandResult.empty();
            case ACCOUNT_NO_SPACE:
                from.sendMessage(Text.of(to.getName()+" doesn't have enough space on his watch."));
                return CommandResult.empty();
            case FAILED:
            case CONTEXT_MISMATCH:
                throw new CommandException(Text.of("Unknown error."));
        }
        return CommandResult.empty();
    }

    public CommandResult start(CommandSource src, CommandContext args) throws CommandException {
        Player p = args.<Player>getOne("player").get();
        if (p != src) args.checkPermission(src, "countdown.start.other");
        CountdownData data = p.getOrCreate(CountdownData.class).get();
        if (data.waitTicks == 0) {
            throw new CommandException(Text.of("This person's timer has already started!"));
        }
        data.waitTicks = 0;
        p.offer(data);
        p.sendTitle(Title.builder()
                .title(Text.of(TextColors.RED, "Your timer has started!"))
                .fadeIn(20)
                .stay(60)
                .fadeOut(20)
                .build());
        p.sendMessage(ChatTypes.SYSTEM, Text.of(TextColors.RED, "Your timer has started!"));
        return CommandResult.success();
    }

    public CommandResult setStart(CommandSource src, CommandContext args) throws CommandException {
        Player p = args.<Player>getOne("player").get();
        CountdownData data = p.getOrCreate(CountdownData.class).get();
        if (data.waitTicks == 0) {
            throw new CommandException(Text.of("This person's timer has already started!"));
        }
        TemporalCurrencies.CurrencyDuration amount = args.<TemporalCurrencies.CurrencyDuration>getOne("time").get();
        long ticks = amount.getDuration().toMillis() / 50;
        data.waitTicks = ticks;
        p.offer(data);
        return CommandResult.success();
    }
}
