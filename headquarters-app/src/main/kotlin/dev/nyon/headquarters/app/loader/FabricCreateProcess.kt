package dev.nyon.headquarters.app.loader

import dev.nyon.headquarters.app.fabricConnector
import dev.nyon.headquarters.app.ktorClient
import dev.nyon.headquarters.app.util.downloadFile
import dev.nyon.headquarters.connector.fabric.models.LauncherMeta
import dev.nyon.headquarters.connector.fabric.requests.getLoaderOfGameAndLoaderVersion
import dev.nyon.headquarters.connector.fabric.requests.getLoaderVersions
import dev.nyon.headquarters.connector.mojang.models.MinecraftVersion
import io.ktor.http.*
import java.nio.file.Path

class FabricCreateProcess(override val profileDir: Path, override val minecraftVersion: MinecraftVersion) :
    LoaderCreateProcess {

    override suspend fun installLibraries() {
        val loaderVersion = fabricConnector.getLoaderOfGameAndLoaderVersion(
            minecraftVersion.id,
            fabricConnector.getLoaderVersions()!!.first().version
        ) ?: error("Cannot find compatible fabric loader for version '$minecraftVersion.id'")

        val meta = loaderVersion.launcherMeta as LauncherMeta
        listOf(meta.libraries.common, meta.libraries.client).flatten().forEach { artifact ->
            val split = artifact.name.split(":")
            val fileName = "${split[1]}-${split[2]}.jar"
            val url = "${artifact.url}${split[0].replace(".", "/")}${
                split.toMutableList().also { it.removeFirst() }.joinToString("/", prefix = "/", postfix = "/")
            }$fileName"
            ktorClient.downloadFile(Url(url), profileDir.resolve("libraries/").resolve(fileName))
        }
    }
}