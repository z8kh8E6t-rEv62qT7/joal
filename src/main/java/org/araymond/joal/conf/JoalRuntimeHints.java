package org.araymond.joal.conf;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.araymond.joal.core.client.emulated.BitTorrentClientConfig;
import org.araymond.joal.core.client.emulated.generator.UrlEncoder;
import org.araymond.joal.core.client.emulated.generator.key.AlwaysRefreshKeyGenerator;
import org.araymond.joal.core.client.emulated.generator.key.KeyGenerator;
import org.araymond.joal.core.client.emulated.generator.key.NeverRefreshKeyGenerator;
import org.araymond.joal.core.client.emulated.generator.key.TimedOrAfterStartedAnnounceRefreshKeyGenerator;
import org.araymond.joal.core.client.emulated.generator.key.TimedRefreshKeyGenerator;
import org.araymond.joal.core.client.emulated.generator.key.TorrentPersistentRefreshKeyGenerator;
import org.araymond.joal.core.client.emulated.generator.key.TorrentVolatileRefreshKeyGenerator;
import org.araymond.joal.core.client.emulated.generator.key.algorithm.DigitRangeTransformedToHexWithoutLeadingZeroAlgorithm;
import org.araymond.joal.core.client.emulated.generator.key.algorithm.HashKeyAlgorithm;
import org.araymond.joal.core.client.emulated.generator.key.algorithm.HashNoLeadingZeroKeyAlgorithm;
import org.araymond.joal.core.client.emulated.generator.key.algorithm.KeyAlgorithm;
import org.araymond.joal.core.client.emulated.generator.key.algorithm.RegexPatternKeyAlgorithm;
import org.araymond.joal.core.client.emulated.generator.peerid.AlwaysRefreshPeerIdGenerator;
import org.araymond.joal.core.client.emulated.generator.peerid.NeverRefreshPeerIdGenerator;
import org.araymond.joal.core.client.emulated.generator.peerid.PeerIdGenerator;
import org.araymond.joal.core.client.emulated.generator.peerid.TimedRefreshPeerIdGenerator;
import org.araymond.joal.core.client.emulated.generator.peerid.TorrentPersistentRefreshPeerIdGenerator;
import org.araymond.joal.core.client.emulated.generator.peerid.TorrentVolatileRefreshPeerIdGenerator;
import org.araymond.joal.core.client.emulated.generator.peerid.generation.PeerIdAlgorithm;
import org.araymond.joal.core.client.emulated.generator.peerid.generation.RandomPoolWithChecksumPeerIdAlgorithm;
import org.araymond.joal.core.client.emulated.generator.peerid.generation.RegexPatternPeerIdAlgorithm;
import org.araymond.joal.core.client.emulated.utils.Casing;
import org.araymond.joal.core.config.AppConfiguration;
import org.araymond.joal.core.torrent.torrent.InfoHash;
import org.araymond.joal.web.messages.incoming.config.Base64TorrentIncomingMessage;
import org.araymond.joal.web.messages.incoming.config.ConfigIncomingMessage;
import org.araymond.joal.web.messages.outgoing.MessagePayload;
import org.araymond.joal.web.messages.outgoing.StompMessage;
import org.araymond.joal.web.messages.outgoing.StompMessageTypes;
import org.araymond.joal.web.messages.outgoing.impl.announce.AnnouncePayload;
import org.araymond.joal.web.messages.outgoing.impl.announce.FailedToAnnouncePayload;
import org.araymond.joal.web.messages.outgoing.impl.announce.SuccessfullyAnnouncePayload;
import org.araymond.joal.web.messages.outgoing.impl.announce.TooManyAnnouncesFailedPayload;
import org.araymond.joal.web.messages.outgoing.impl.announce.WillAnnouncePayload;
import org.araymond.joal.web.messages.outgoing.impl.config.ConfigHasBeenLoadedPayload;
import org.araymond.joal.web.messages.outgoing.impl.config.ConfigIsInDirtyStatePayload;
import org.araymond.joal.web.messages.outgoing.impl.config.InvalidConfigPayload;
import org.araymond.joal.web.messages.outgoing.impl.config.ListOfClientFilesPayload;
import org.araymond.joal.web.messages.outgoing.impl.files.FailedToAddTorrentFilePayload;
import org.araymond.joal.web.messages.outgoing.impl.files.TorrentFileAddedPayload;
import org.araymond.joal.web.messages.outgoing.impl.files.TorrentFileDeletedPayload;
import org.araymond.joal.web.messages.outgoing.impl.global.state.GlobalSeedStartedPayload;
import org.araymond.joal.web.messages.outgoing.impl.global.state.GlobalSeedStoppedPayload;
import org.araymond.joal.web.messages.outgoing.impl.speed.SeedingSpeedHasChangedPayload;

/** JSON roots loaded from external files or sent through STOMP, including polymorphic implementations. */
public class JoalRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
        new BindingReflectionHintsRegistrar().registerReflectionHints(hints.reflection(),
                BitTorrentClientConfig.class,
                UrlEncoder.class,
                AlwaysRefreshKeyGenerator.class,
                KeyGenerator.class,
                NeverRefreshKeyGenerator.class,
                TimedOrAfterStartedAnnounceRefreshKeyGenerator.class,
                TimedRefreshKeyGenerator.class,
                TorrentPersistentRefreshKeyGenerator.class,
                TorrentVolatileRefreshKeyGenerator.class,
                DigitRangeTransformedToHexWithoutLeadingZeroAlgorithm.class,
                HashKeyAlgorithm.class,
                HashNoLeadingZeroKeyAlgorithm.class,
                KeyAlgorithm.class,
                RegexPatternKeyAlgorithm.class,
                AlwaysRefreshPeerIdGenerator.class,
                NeverRefreshPeerIdGenerator.class,
                PeerIdGenerator.class,
                TimedRefreshPeerIdGenerator.class,
                TorrentPersistentRefreshPeerIdGenerator.class,
                TorrentVolatileRefreshPeerIdGenerator.class,
                PeerIdAlgorithm.class,
                RandomPoolWithChecksumPeerIdAlgorithm.class,
                RegexPatternPeerIdAlgorithm.class,
                Casing.class,
                AppConfiguration.class,
                InfoHash.class,
                Base64TorrentIncomingMessage.class,
                ConfigIncomingMessage.class,
                MessagePayload.class,
                StompMessage.class,
                StompMessageTypes.class,
                AnnouncePayload.class,
                FailedToAnnouncePayload.class,
                SuccessfullyAnnouncePayload.class,
                TooManyAnnouncesFailedPayload.class,
                WillAnnouncePayload.class,
                ConfigHasBeenLoadedPayload.class,
                ConfigIsInDirtyStatePayload.class,
                InvalidConfigPayload.class,
                ListOfClientFilesPayload.class,
                FailedToAddTorrentFilePayload.class,
                TorrentFileAddedPayload.class,
                TorrentFileDeletedPayload.class,
                GlobalSeedStartedPayload.class,
                GlobalSeedStoppedPayload.class,
                SeedingSpeedHasChangedPayload.class
        );
        hints.resources().registerPattern("public/**");
        hints.resources().registerPattern("org/springframework/boot/logging/log4j2/*.xml");
        hints.resources().registerPattern("META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat");
        // Boot's Log4j plugins predate its native-image support; Log4j supplies its own plugins' hints.
        hints.reflection().registerType(org.springframework.boot.logging.log4j2.ColorConverter.class,
                org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS);
        hints.reflection().registerType(org.springframework.boot.logging.log4j2.WhitespaceThrowablePatternConverter.class,
                org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS);
        hints.reflection().registerType(org.springframework.boot.logging.log4j2.ExtendedWhitespaceThrowablePatternConverter.class,
                org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS);
        hints.reflection().registerType(org.springframework.boot.logging.log4j2.EnclosedInSquareBracketsConverter.class,
                org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS);
        hints.reflection().registerType(org.springframework.boot.logging.log4j2.CorrelationIdConverter.class,
                org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS);
        hints.reflection().registerType(org.springframework.aot.hint.TypeReference.of("org.springframework.boot.logging.log4j2.SpringEnvironmentLookup"),
                org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    }
}
