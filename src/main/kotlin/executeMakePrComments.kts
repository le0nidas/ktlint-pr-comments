import kotlin.system.exitProcess

//DEPS com.squareup.moshi:moshi:1.9.3
//DEPS com.squareup.moshi:moshi-kotlin:1.9.3
//DEPS com.squareup.retrofit2:retrofit:2.9.0
//DEPS com.squareup.retrofit2:converter-moshi:2.9.0

//INCLUDE common.kt
//INCLUDE makePrComments.kt
//INCLUDE createGithubEvent.kt

val result = makePrComments(args)
if (result != 0) {
    exitProcess(result)
}

println("Comments were made!")