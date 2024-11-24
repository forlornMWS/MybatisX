buildscript {
    repositories {
        mavenLocal()
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
        maven { url = uri("https://dl.bintray.com/jetbrains/intellij-plugin-service") }
        maven { url = uri("https://dl.bintray.com/jetbrains/intellij-third-party-dependencies/") }
    }
    dependencies {
        classpath("org.jetbrains.intellij.plugins:gradle-intellij-plugin:0.7.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.32")
    }
}

plugins {
    java
    kotlin("jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

intellij {
    version.set("2023.2.8") // IntelliJ IDEA版本
    type.set("IU") // 企业版
    plugins.set(
        listOf(
            "java",
            "Kotlin",
            "Spring",
            "DatabaseTools",
            "com.intellij.spring.boot"
        )
    ) // Bundled plugin dependencies
    pluginName.set("MybatisX")
    sandboxDir.set("${rootProject.rootDir}/idea-sandbox")
    updateSinceUntilBuild.set(false)
    downloadSources.set(true)
}

group = "com.baomidou.plugin.idea.mybatisx"
version = "1.6.4"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("com.softwareloop:mybatis-generator-lombok-plugin:1.0")
    implementation("uk.com.robust-it:cloning:1.9.2")
    implementation("org.mybatis.generator:mybatis-generator-core:1.4.0")
    implementation("org.freemarker:freemarker:2.3.30")
    implementation("com.itranswarp:compiler:1.0")
    testImplementation("junit:junit:4.13.1")
    testImplementation("commons-io:commons-io:2.14.0")
    compileOnly("org.projectlombok:lombok:1.18.20")
    annotationProcessor("org.projectlombok:lombok:1.18.20")
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
//    patchPluginXml {
//        sinceBuild.set("232")
//        untilBuild.set("243.*")
//    }
}
