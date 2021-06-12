plugins {
    id("com.github.minigdx.jvm") version "LATEST-SNAPSHOT"
    id("com.github.minigdx.js") version "LATEST-SNAPSHOT"
    id("com.github.minigdx.common") version "LATEST-SNAPSHOT"

    id("org.asciidoctor.jvm.convert") version "3.1.0"
}

minigdx {
    jvm.mainClass.set("com.github.minigdx.docs.quick.start.Main")
    this.version.set("LATEST-SNAPSHOT")
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
