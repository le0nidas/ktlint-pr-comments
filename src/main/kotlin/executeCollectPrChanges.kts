import kotlin.system.exitProcess

//DEPS com.squareup.moshi:moshi:1.9.3
//DEPS com.squareup.moshi:moshi-kotlin:1.9.3
//DEPS com.squareup.retrofit2:retrofit:2.9.0
//DEPS com.squareup.retrofit2:converter-moshi:2.9.0

//INCLUDE collectPrChanges.kt

val result = collectPrChanges(args)

if (result.first != 0) {
    System.err.print(result.second)
    exitProcess(result.first)
}

print(result.second)