package com.proksi.kotodama.objects

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions

class CloudFunction {



    fun callEnterReferral(data: Map<String, Any>, completion: (Result<Unit>) -> Unit) {
        val functions = FirebaseFunctions.getInstance()

        Log.d("cloud", "callEnterReferral: called $data  ")
        functions
            .getHttpsCallable("enterReferral")
            .call(data)
            .addOnSuccessListener {
                completion(Result.success(Unit))
                Log.d("cloud", "callEnterReferral: called  ")


            }
            .addOnFailureListener { error ->
                completion(Result.failure(error))
                Log.d("cloud", "callEnterReferral: called $error ")
            }
    }

}