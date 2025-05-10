plugins {
	alias(libs.plugins.android.library)
	alias(libs.plugins.kotlin.android)
}

android {
	namespace = "xyz.thespud.skimap"
	compileSdk = 36

	defaultConfig {

		minSdk = 27
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		consumerProguardFiles("consumer-rules.pro")
		// targetSdk = 36
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