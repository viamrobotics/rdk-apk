package com.viam.rdk.fgservice

import android.util.Log

private const val TAG = "LogStreamState"

/** state machine for filtering tracebacks out of log streams */
class LogStreamState(val name: String, val transitions: Map<String, Map<Regex, String>>) {
    var state = "" // empty is initial state

    /** returns the state of this line */
    fun process(line: String): Pair<String, String> {
        transitions[this.state]?.map { pair ->
            pair.key.find(line)?.let {
                Log.i(TAG, "transition $name '${this.state}' -> '${pair.value}' $line")
                val ret = Pair(this.state, pair.value)
                this.state = pair.value
                return ret
            }
        }
        return Pair(this.state, this.state)
    }

    fun clear() {
        this.state = ""
    }
}

val infoStream = LogStreamState("info", mapOf(
    Pair("", mapOf(
        Pair(Regex("backtrace at robot shutdown"), "trace"),
    )),
    Pair("trace", mapOf(
        Pair(Regex("^\\s*$"), "trace-empty"),
    )),
    Pair("trace-empty", mapOf(
        Pair(Regex("^goroutine \\d+"), "trace"),
        Pair(Regex("^(?!goroutine)"), ""),
    )),
))

val errorStream = LogStreamState("error", mapOf(
    Pair("", mapOf(
        Pair(Regex("^goroutine leak\\(s\\) detected: found unexpected goroutines"), "trace")
    )),
    Pair("trace", mapOf(
        Pair(Regex("^\\]"), ""),
    )),
))
