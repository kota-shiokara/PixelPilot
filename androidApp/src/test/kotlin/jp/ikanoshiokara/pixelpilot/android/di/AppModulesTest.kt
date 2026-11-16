package jp.ikanoshiokara.pixelpilot.android.di

import android.app.Application
import android.content.Context
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

/**
 * Koin の定義が解決できるか(コンストラクタの引数がすべて定義されているか)を検証する。
 * 定義を追加したときの登録漏れを、実機で起動する前に見つけるため。
 */
@OptIn(KoinExperimentalAPI::class)
class AppModulesTest {
    @Test
    fun appModulesCanBeResolved() {
        module { includes(appModules) }.verify(
            // startKoin の androidContext() で与えられる型
            extraTypes = listOf(Context::class, Application::class),
        )
    }
}
