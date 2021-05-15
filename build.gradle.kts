plugins {
    id("com.github.minigdx.jvm") version "DEV-SNAPSHOT"
    id("com.github.minigdx.js") version "DEV-SNAPSHOT"
    id("com.github.minigdx.common") version "DEV-SNAPSHOT"

    id("org.asciidoctor.jvm.convert") version "3.1.0"
}

minigdx {
    jvm.mainClass.set("your.game.Main")
}

val copyJs = project.tasks.register("unzipBundleJs", Copy::class) {
    this.dependsOn("bundle-js")
    this.from(project.zipTree("build/minigdx/minigdx-docs.zip"))
    this.into("build/docs/asciidoc/")

}
project.tasks.getByName("asciidoctor").dependsOn(copyJs)
project.tasks.getByName("asciidoctor", org.asciidoctor.gradle.jvm.AsciidoctorTask::class) {
    this.baseDirFollowsSourceDir()

}
