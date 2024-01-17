package com.azuresamples.msaldelegatedandroidkotlinsampleapp

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.azuresamples.msaldelegatedandroidkotlinsampleapp.databinding.ActivityMainBinding
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var authClient: IMultipleAccountPublicClientApplication
    private lateinit var accountList: List<IAccount>
    private var accessToken: String? = null
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val WEB_API_BASE_URL = "" // Developers should set the respective URL of their web API here
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        CoroutineScope(Dispatchers.Main).launch {
            authClient = initClient()
            accountList = getAccounts()
            updateUI(accountList)
        }

        initializeButtonListeners()
    }

    private fun initializeButtonListeners() {
        binding.btnAcquireTokenInteractively.setOnClickListener {
            acquireTokenInteractively()
        }

        binding.btnAcquireTokenSilently.setOnClickListener {
            acquireTokenSilently()
        }

        binding.btnRemoveAccount.setOnClickListener {
            removeAccount()
        }

        binding.btnAccessApi.setOnClickListener {
            accessWebApi()
        }
    }

    private fun acquireTokenInteractively() {
        binding.txtLog.text = ""

        val scopes = binding.scope.text.toString().lowercase().split(" ")
        val parameters = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(this@MainActivity)
            .withScopes(scopes)
            .withCallback(getAuthInteractiveCallback())
            .build()

        authClient.acquireToken(parameters)
    }

    private fun acquireTokenSilently() {
        binding.txtLog.text = ""

        val scopes = binding.scope.text.toString().lowercase().split(" ")
        val selectedAccount: IAccount = accountList[binding.accountList.selectedItemPosition]
        val parameters = AcquireTokenSilentParameters.Builder()
            .forAccount(selectedAccount)
            .fromAuthority(selectedAccount.authority)
            .withScopes(scopes)
            .forceRefresh(false)
            .withCallback(getAuthSilentCallback())
            .build()

        authClient.acquireTokenSilentAsync(parameters)
    }

    private fun removeAccount() {
        binding.txtLog.text = ""

        val selectedAccount: IAccount = accountList[binding.accountList.selectedItemPosition]

        authClient.removeAccount(selectedAccount, removeAccountCallback())
    }

    private fun accessWebApi() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.txtLog.text = ""
            try {
                if (WEB_API_BASE_URL.isBlank()) {
                    Toast.makeText(this@MainActivity, getString(R.string.message_web_base_url), Toast.LENGTH_LONG).show()
                    return@launch
                }
                val apiResponseCode = withContext(Dispatchers.IO) {
                    ApiClient.performGetApiRequest(WEB_API_BASE_URL, accessToken)
                }
                binding.txtLog.text = getString(R.string.log_web_api_response)  + apiResponseCode
            } catch (exception: Exception) {
                Log.d(TAG, "Exception at accessing web API: $exception")

                binding.txtLog.text = getString(R.string.exception_web_api) + exception
            }
        }
    }

    private fun getAuthInteractiveCallback(): AuthenticationCallback {
        return object : AuthenticationCallback {

            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                /* Successfully got a token, use it to call a protected resource - Web API */
                Log.d(TAG, "Successfully authenticated")
                Log.d(TAG, "ID Token: " + authenticationResult.account.claims?.get("id_token"))

                /* Reload account asynchronously to get the up-to-date list. */
                CoroutineScope(Dispatchers.Main).launch {
                    accessToken = authenticationResult.accessToken
                    accountList = getAccounts()

                    updateUI(accountList)
                    binding.txtLog.text = getString(R.string.log_token_interactive) +  accessToken
                }
            }

            override fun onError(exception: MsalException) {
                /* Failed to acquireToken */
                Log.d(TAG, "Authentication failed: $exception")

                accessToken = null
                binding.txtLog.text = getString(R.string.exception_authentication) + exception
            }

            override fun onCancel() {
                // Do nothing
            }
        }
    }

    private fun getAuthSilentCallback(): SilentAuthenticationCallback {
        return object : SilentAuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                Log.d(TAG, "Successfully authenticated")

                accessToken = authenticationResult?.accessToken
                binding.txtLog.text = getString(R.string.log_token_silent) + accessToken
            }

            override fun onError(exception: MsalException?) {
                /* Failed to acquireToken */
                Log.d(TAG, "Authentication failed: $exception")

                accessToken = null
                binding.txtLog.text = getString(R.string.exception_authentication) + exception
            }

        }
    }

    private fun removeAccountCallback(): IMultipleAccountPublicClientApplication.RemoveAccountCallback {
        return object : IMultipleAccountPublicClientApplication.RemoveAccountCallback {
            override fun onRemoved() {
                CoroutineScope(Dispatchers.Main).launch {
                    accessToken = null
                    accountList = getAccounts()

                    updateUI(accountList)
                    Toast.makeText(this@MainActivity, getString(R.string.exception_remove_account), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(exception: MsalException) {
                accessToken = null
                binding.txtLog.text = getString(R.string.exception_remove_account) + exception
            }
        }

    }

    private suspend fun initClient(): IMultipleAccountPublicClientApplication = withContext(Dispatchers.IO) {
        return@withContext PublicClientApplication.createMultipleAccountPublicClientApplication(
            this@MainActivity,
            R.raw.auth_config_ciam
        )
    }
    private suspend fun getAccounts(): List<IAccount> = withContext(Dispatchers.IO) {
        return@withContext authClient.accounts
    }

    private fun updateUI(accounts : List<IAccount>) {
        if (accounts.isNotEmpty()) {
            binding.btnRemoveAccount.isEnabled = true
            binding.btnAccessApi.isEnabled = true
            binding.btnAcquireTokenSilently.isEnabled = true
            binding.btnAcquireTokenInteractively.isEnabled = true
        } else {
            binding.btnRemoveAccount.isEnabled = false
            binding.btnAccessApi.isEnabled = false
            binding.btnAcquireTokenSilently.isEnabled = true
            binding.btnAcquireTokenInteractively.isEnabled = true
        }

        val dataAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item,
            accounts.map { it.username }.toMutableList()
        )

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.accountList.adapter = dataAdapter
        dataAdapter.notifyDataSetChanged()
    }
}
