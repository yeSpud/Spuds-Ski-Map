plugins {
	alias(libs.plugins.android.library)
	alias(libs.plugins.kotlin.android)
	`maven-publish`
}

android {
	namespace = "xyz.thespud.skimap"
	compileSdk = 36

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
	kotlinOptions {
		jvmTarget = "11"
	}

	publishing {
		singleVariant("release") {
			withSourcesJar()
		}
	}
}

publishing {
	publications {
		register<MavenPublication>("release") {
			groupId = "xyz.thespud"
			artifactId = "spuds-ski-map"
			version = "2025.06.01"

			afterEvaluate {
				from(components["release"])
			}
			pom {
				name = "Spud's Ski Map"
				description = "An android library for setting up ski maps with Google Maps"
				url = "https://github.com/yeSpud/Spuds-Ski-Map"
				licenses {
					license {
						name = "MIT License"
						url = "https://github.com/yeSpud/Spuds-Ski-Map/blob/main/LICENSE"
					}
				}
				developers {
					developer {
						id = "yeSpud"
						name = "Spud"
					}
				}
			}
		}
	}
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