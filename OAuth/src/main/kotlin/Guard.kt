import spark.Spark

fun guard(obj: Any?, status: Int = 400, message: String = ""): Boolean {
    return guard(obj != null, status, message)
}

fun guard(cond: Boolean, status: Int = 400, message: String = ""): Boolean {
    if (!cond) Spark.halt(status, message)
    return true
}
