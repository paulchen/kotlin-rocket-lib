package at.rueckgr.kotlin.rocketbot.util

import at.rueckgr.kotlin.rocketbot.generated.statistics.model.GetApiV1StatisticsList200Response
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertEquals

class StatisticsDeserializationTest {
    @Test
    fun `statistics list supports memory sizes exceeding 32 bit integers`() {
        val response = jacksonObjectMapper().readValue<GetApiV1StatisticsList200Response>(
            """{"success":true,"statistics":[{"os":{"totalmem":33293393920,"freemem":17179869184}}]}""",
        )

        val os = response.statistics!!.single().os!!
        assertEquals(33_293_393_920L, os.totalmem)
        assertEquals(17_179_869_184L, os.freemem)
    }
}
