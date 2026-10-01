package com.hatuka.swipeclean.ui.donate

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hatuka.swipeclean.R
import com.hatuka.swipeclean.donate.TipJar
import com.hatuka.swipeclean.donate.TipJarState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DonateViewModel @Inject constructor(private val tipJar: TipJar) : ViewModel() {
    val state: StateFlow<TipJarState> = tipJar.state
    val thanks: SharedFlow<Unit> = tipJar.thanks

    init {
        tipJar.connect()
    }

    fun donate(activity: Activity, productId: String) = tipJar.donate(activity, productId)
}

private data class TipOption(val productId: String, @StringRes val label: Int)

private val TIP_OPTIONS = listOf(
    TipOption("tip_small", R.string.donate_tip_small),
    TipOption("tip_medium", R.string.donate_tip_medium),
    TipOption("tip_large", R.string.donate_tip_large),
)

@Composable
fun DonateRoute(onBack: () -> Unit, viewModel: DonateViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    var thanked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.thanks.collect { thanked = true } }
    DonateContent(
        state = state,
        thanked = thanked,
        onDonate = { id -> activity?.let { viewModel.donate(it, id) } },
        onBack = onBack,
    )
}

/** "Enjoying the app?" page: three donation amounts, paid through Google Play. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateContent(state: TipJarState, thanked: Boolean, onDonate: (String) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.donate_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color(0xFFE5484D), modifier = Modifier.size(72.dp))
            Text(
                stringResource(R.string.donate_headline),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(stringResource(R.string.donate_body), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)

            if (thanked) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        stringResource(R.string.donate_thanks),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    )
                }
            }

            val prices = (state as? TipJarState.Ready)?.tips?.associate { it.productId to it.price }.orEmpty()
            TIP_OPTIONS.forEach { option ->
                val price = prices[option.productId]
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(option.label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Button(onClick = { onDonate(option.productId) }, enabled = price != null) {
                            Text(price ?: stringResource(R.string.donate_give))
                        }
                    }
                }
            }
            when (state) {
                TipJarState.Loading -> CircularProgressIndicator()
                TipJarState.Unavailable -> Text(
                    stringResource(R.string.donate_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                is TipJarState.Ready -> Unit
            }
            TextButton(onClick = onBack) { Text(stringResource(R.string.donate_later)) }
        }
    }
}
