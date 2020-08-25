import kotlin.system.exitProcess

//DEPS com.squareup.moshi:moshi:1.9.3
//DEPS com.squareup.moshi:moshi-kotlin:1.9.3
//DEPS com.squareup.retrofit2:retrofit:2.9.0
//DEPS com.squareup.retrofit2:converter-moshi:2.9.0

//INCLUDE logging.kt
//INCLUDE common.kt
//INCLUDE collectPrChanges.kt
//INCLUDE createGithubEvent.kt

val result = collectPrChanges(args)

if (result != 0) {
    exitProcess(result)
}

println("Changes collected")