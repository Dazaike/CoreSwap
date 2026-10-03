package com.coreswap.app.ui.showcase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.coreswap.app.ui.GlassDropdown
import com.coreswap.app.ui.GlassPasswordField
import com.coreswap.app.ui.GlassSubmitButton
import com.coreswap.app.ui.GlassTextField
import com.coreswap.app.ui.LocalToasts
import com.coreswap.app.ui.ToastKind
import kotlinx.coroutines.delay

private val EmailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

private val Countries = listOf(
    "Argentina", "Australia", "Austria", "Belgium", "Brazil", "Canada", "Chile", "China",
    "Denmark", "Egypt", "Finland", "France", "Germany", "Greece", "India", "Indonesia",
    "Ireland", "Italy", "Japan", "Kenya", "Mexico", "Netherlands", "New Zealand", "Norway",
    "Poland", "Portugal", "South Africa", "Spain", "Sweden", "Vietnam",
)

@Composable
fun InputsPage(backdrop: Backdrop) {
    val toasts = LocalToasts.current
    val focusManager = LocalFocusManager.current

    val name = rememberTextFieldState()
    val email = rememberTextFieldState()
    val password = rememberTextFieldState()
    val bio = rememberTextFieldState()
    val accountId = rememberTextFieldState("PC-2048")

    var nameTouched by rememberSaveable { mutableStateOf(false) }
    var emailTouched by rememberSaveable { mutableStateOf(false) }
    var passwordTouched by rememberSaveable { mutableStateOf(false) }
    var country by rememberSaveable { mutableIntStateOf(-1) }

    fun nameError(touched: Boolean) = if (touched && name.text.isBlank()) "Name is required" else null
    fun emailError(touched: Boolean) =
        if (touched && !EmailPattern.matches(email.text)) "Enter a valid email address" else null
    fun passwordError(touched: Boolean) =
        if (touched && password.text.length in 1..7) "Use at least 8 characters" else null

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ShowcaseSection("Create an account", caption = "Fields validate when you leave them or submit") {
            GlassTextField(
                state = name,
                label = "Name",
                modifier = Modifier.fillMaxWidth().then(onBlur { nameTouched = true }),
                supportingText = "Shown on your profile",
                error = nameError(nameTouched),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                onSubmit = {
                    nameTouched = true
                    focusManager.moveFocus(FocusDirection.Down)
                },
            )
            Spacer(Modifier.height(8.dp))
            GlassTextField(
                state = email,
                label = "Email",
                modifier = Modifier.fillMaxWidth().then(onBlur { emailTouched = true }),
                error = emailError(emailTouched),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                onSubmit = {
                    emailTouched = true
                    focusManager.moveFocus(FocusDirection.Down)
                },
            )
            Spacer(Modifier.height(8.dp))
            GlassDropdown(
                label = "Country",
                options = Countries,
                selectedIndex = country,
                onSelect = { country = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            GlassPasswordField(
                state = password,
                label = "Password",
                modifier = Modifier.fillMaxWidth().then(onBlur { passwordTouched = true }),
                supportingText = "At least 8 characters",
                error = passwordError(passwordTouched),
                onSubmit = {
                    passwordTouched = true
                    focusManager.moveFocus(FocusDirection.Down)
                },
            )
            Spacer(Modifier.height(8.dp))
            GlassTextField(
                state = bio,
                label = "Bio",
                modifier = Modifier.fillMaxWidth(),
                supportingText = "Optional",
                singleLine = false,
                maxLength = 140,
            )
            Spacer(Modifier.height(8.dp))
            GlassTextField(
                state = accountId,
                label = "Account ID",
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
            Spacer(Modifier.height(16.dp))
            GlassSubmitButton(backdrop, "Create account", onSubmit = {
                nameTouched = true
                emailTouched = true
                passwordTouched = true
                val n = listOf(nameError(true), emailError(true), passwordError(true)).count { it != null }
                if (n > 0) {
                    toasts.show("Fix $n field(s)", ToastKind.Error)
                } else {
                    delay(1500)
                    toasts.show("Account created", ToastKind.Success)
                }
            })
        }
    }
}

/** Calls [action] when focus leaves the field (or anything inside it) after having been there. */
@Composable
private fun onBlur(action: () -> Unit): Modifier {
    val current by rememberUpdatedState(action)
    val hadFocus = remember { booleanArrayOf(false) }
    return remember {
        Modifier.onFocusChanged { state ->
            if (hadFocus[0] && !state.hasFocus) current()
            hadFocus[0] = state.hasFocus
        }
    }
}
