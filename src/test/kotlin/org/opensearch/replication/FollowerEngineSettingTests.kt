/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.replication

import org.opensearch.cluster.metadata.IndexMetadata
import org.opensearch.common.settings.Settings
import org.opensearch.core.index.Index
import org.opensearch.test.IndexSettingsModule
import org.opensearch.test.OpenSearchTestCase

class FollowerEngineSettingTests : OpenSearchTestCase() {

    private fun plugin(): ReplicationPlugin = ReplicationPlugin()

    private fun indexSettings(follower: Boolean, node: Settings) =
        IndexSettingsModule.newIndexSettings(
            Index("follower", "uuid"),
            Settings.builder()
                .put(IndexMetadata.SETTING_VERSION_CREATED, org.opensearch.Version.CURRENT)
                .apply { if (follower) put(ReplicationPlugin.REPLICATED_INDEX_SETTING.key, "leader:idx") }
                .build(),
            node
        )

    fun `test default setting is replication`() {
        assertEquals(
            ReplicationPlugin.FOLLOWER_ENGINE_REPLICATION,
            ReplicationPlugin.REPLICATION_FOLLOWER_ENGINE.get(Settings.EMPTY)
        )
    }

    fun `test unknown value is rejected`() {
        val bad = Settings.builder().put(ReplicationPlugin.REPLICATION_FOLLOWER_ENGINE.key, "other").build()
        val e = expectThrows(IllegalArgumentException::class.java) { ReplicationPlugin.REPLICATION_FOLLOWER_ENGINE.get(bad) }
        assertTrue(e.message!!, e.message!!.contains("must be [replication] or [delegate]"))
    }

    fun `test replication engine serves followers by default`() {
        assertTrue(plugin().getEngineFactory(indexSettings(true, Settings.EMPTY)).isPresent)
    }

    fun `test non follower index never gets replication engine`() {
        assertFalse(plugin().getEngineFactory(indexSettings(false, Settings.EMPTY)).isPresent)
    }

    fun `test delegate leaves followers to another engine plugin`() {
        val node = Settings.builder().put(ReplicationPlugin.REPLICATION_FOLLOWER_ENGINE.key, "delegate").build()
        assertFalse(plugin().getEngineFactory(indexSettings(true, node)).isPresent)
    }
}
