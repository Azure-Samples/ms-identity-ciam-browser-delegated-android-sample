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
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    companion object {
        private val TAG = MainActivity::class.java.simpleName
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

        init()
    }

    private fun init() {
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
    }

    private fun acquireTokenInteractively() {
        val scopes = binding.scope.text.toString().lowercase().split(" ")

        binding.txtLog.text = ""

        authClient.acquireToken(AcquireTokenParameters(
            AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(this@MainActivity)
                .withScopes(scopes)
                .withCallback(getAuthInteractiveCallback())
        ))
    }

    private fun acquireTokenSilently() {
        val scopes = binding.scope.text.toString().lowercase().split(" ")
        val selectedAccount: IAccount = accountList[binding.accountList.selectedItemPosition]

        binding.txtLog.text = ""

        authClient.acquireTokenSilentAsync(AcquireTokenSilentParameters(
            AcquireTokenSilentParameters.Builder()
                .forAccount(selectedAccount)
                .fromAuthority(selectedAccount.authority)
                .withScopes(scopes)
                .forceRefresh(false)
                .withCallback(getAuthSilentCallback())
        ))
    }

    private fun removeAccount() {
        val selectedAccount: IAccount = accountList[binding.accountList.selectedItemPosition]

        binding.txtLog.text = ""

        authClient.removeAccount(selectedAccount, removeAccountCallback())
    }

    private fun getAuthInteractiveCallback(): AuthenticationCallback {
        return object : AuthenticationCallback {

            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                /* Successfully got a token, use it to call a protected resource - MSGraph */
                Log.d(TAG, "Successfully authenticated")
                Log.d(TAG, "ID Token: " + authenticationResult.account.claims?.get("id_token"))

                binding.txtLog.text = "Interactive Request Success:\n" + authenticationResult?.accessToken

                /* Reload account asynchronously to get the up-to-date list. */
                CoroutineScope(Dispatchers.Main).launch {
                    accountList = getAccounts()
                    updateUI(accountList)
                }
            }

            override fun onError(exception: MsalException) {
                /* Failed to acquireToken */
                Log.d(TAG, "Authentication failed: $exception")

                binding.txtLog.text = "Authentication failed:" + exception
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

                binding.txtLog.text = "Silent Request Success:\n" + authenticationResult?.accessToken
            }

            override fun onError(exception: MsalException?) {
                /* Failed to acquireToken */
                Log.d(TAG, "Authentication failed: $exception")

                binding.txtLog.text = "Authentication failed: $exception"
            }

        }
    }

    private fun removeAccountCallback(): IMultipleAccountPublicClientApplication.RemoveAccountCallback {
        return object : IMultipleAccountPublicClientApplication.RemoveAccountCallback {
            override fun onRemoved() {
                Toast.makeText(this@MainActivity, "Account removed.", Toast.LENGTH_SHORT).show()

                CoroutineScope(Dispatchers.Main).launch {
                    accountList = getAccounts()
                    updateUI(accountList)
                }
            }

            override fun onError(exception: MsalException) {
                binding.txtLog.text = "MSAL Exception:" + exception
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
            binding.btnAcquireTokenSilently.isEnabled = true
            binding.btnAcquireTokenInteractively.isEnabled = true
        } else {
            binding.btnRemoveAccount.isEnabled = false
            binding.btnAcquireTokenSilently.isEnabled = true
            binding.btnAcquireTokenInteractively.isEnabled = true
        }

        val dataAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item,
            accounts.mapNotNull { it.username }.toMutableList()
        )

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.accountList.adapter = dataAdapter
        dataAdapter.notifyDataSetChanged()
    }
}
