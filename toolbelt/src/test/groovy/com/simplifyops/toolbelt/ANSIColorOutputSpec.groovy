package com.simplifyops.toolbelt

import spock.lang.Specification

import static com.simplifyops.toolbelt.ANSIColorOutput.Color.BLUE
import static com.simplifyops.toolbelt.ANSIColorOutput.Color.RED
import static com.simplifyops.toolbelt.ANSIColorOutput.Color.RESET

/**
 * Created by greg on 7/8/16.
 */
class ANSIColorOutputSpec extends Specification {
    def "toColors null"() {
        when:
        def result = ANSIColorOutput.toColors(null)
        then:
        result == null
    }

    def "toColors string"() {
        when:
        def result = ANSIColorOutput.toColors("input")
        then:
        result == "input"
    }

    class MyColorString implements ANSIColorOutput.ColorString {
        Set<ANSIColorOutput.ColorArea> colors
        String value

        @Override
        public String toString() {
            value
        }
    }
    class MyColorArea implements ANSIColorOutput.ColorArea{
        int start=0
        int length=-1
        ANSIColorOutput.Color color
    }

    def "toColors ColorString"() {
        given:
        def cstring = new MyColorString(
                value: 'test',
                colors: [
                        new MyColorArea(color:RED)
                ]
        )
        when:
        def result = ANSIColorOutput.toColors(cstring)
        then:
        result == "${RED}test$RESET".toString()
    }
    def "toColors ColorString multiple"() {
        given:
        def cstring = new MyColorString(
                value: 'test',
                colors: [
                        new MyColorArea(color: RED, start: 0, length: 2),
                        new MyColorArea(color: BLUE, start: 2, length: -1)

                ]
        )
        when:
        def result = ANSIColorOutput.toColors(cstring)
        then:
        result == "${RED}te${RESET}${BLUE}st${RESET}".toString()
    }
}
