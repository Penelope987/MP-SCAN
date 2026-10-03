package online.mpscan.app.data
import android.graphics.Bitmap
import kotlinx.coroutines.runBlocking
import online.mpscan.app.ui.CoverBlur
import coil3.size.Size
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DiscoveryPreferencesTest {
 @Before fun clear(){SettingsStore(RuntimeEnvironment.getApplication()).prefs.edit().clear().commit()}
 @Test fun adultPreferenceCanBeRestoredAfterRestart(){val context=RuntimeEnvironment.getApplication();SettingsStore(context).adultDisplay="hide";assertEquals("hide",SettingsStore(context).adultDisplay);SettingsStore(context).adultDisplay="blur";assertEquals("blur",SettingsStore(context).adultDisplay);SettingsStore(context).adultDisplay="show";assertEquals("show",SettingsStore(context).adultDisplay)}
 @Test fun onboardingCompletionAndVotesPersist(){val context=RuntimeEnvironment.getApplication();val store=SettingsStore(context);store.choose("one","like");store.choose("two","no");store.choose("three","skip");store.discoveryDone=true;val restored=SettingsStore(context);assertTrue(restored.discoveryDone);assertEquals(mapOf("one" to "like","two" to "no","three" to "skip"),restored.choices());restored.choose("one","no");assertEquals(3,restored.choices().size);assertEquals("no",restored.choices()["one"])}
 @Test fun blurTransformsPixelsOnAndroid28WithoutChangingOriginal()=runBlocking{val input=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888);for(y in 0 until 96)for(x in 0 until 96)input.setPixel(x,y,if((x/4+y/4)%2==0)android.graphics.Color.WHITE else android.graphics.Color.BLACK);val output=CoverBlur().transform(input,Size(96,96));val value=android.graphics.Color.red(output.getPixel(48,48));assertTrue(value in 20..235);assertEquals(android.graphics.Color.WHITE,input.getPixel(48,48));assertEquals(96,output.width)}
 @Test fun coverClassificationPersistsForOfflineHistory(){val context=RuntimeEnvironment.getApplication();val work=Work("adult","Obra","","https://cover","https://banner","","","",emptyList(),0,0,adult=true);SettingsStore(context).cacheAdultCovers(listOf(work));assertEquals(setOf("https://cover","https://banner"),SettingsStore(context).adultCovers())}
}
