import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.android.library)
	alias(libs.plugins.maven.publish)
	signing
}

android {
	namespace = "xyz.thespud.skimap"
	compileSdk = 37

	defaultConfig {

		minSdk = 27
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		consumerProguardFiles("consumer-rules.pro")
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}
	compileSdkMinor = 0
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.fromTarget("11")
		freeCompilerArgs.addAll(listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode"))
	}
}

mavenPublishing {
	publishToMavenCentral()
	signAllPublications()
}

dependencies {
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.appcompat)
	implementation(libs.material)
	implementation(libs.play.services.maps)
	implementation(libs.android.maps.utils)
	implementation(libs.maps.ktx)
	implementation(libs.maps.utils.ktx)
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
}