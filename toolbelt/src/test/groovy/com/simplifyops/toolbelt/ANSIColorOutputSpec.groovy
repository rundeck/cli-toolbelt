package com.simplifyops.toolbelt

import spock.lang.Specification

import static com.simplifyops.toolbelt.ANSIColorOutput.Color.BLUE
import static com.simplifyops.toolbelt.ANSIColorOutput.Color.BOLD
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

    def "template"() {

        when:
        def result = ANSIColorOutput.colorizeTemplate(template)

        then:
        result.colors.sort()[0].color == RED
        result.colors.sort()[0].start == 5
        result.colors.sort()[0].length == 6
        result.colors.sort()[1].color == BLUE
        result.colors.sort()[1].start == 15
        result.colors.sort()[1].length == 4

        result.toString() == plain
        where:
        template                               | plain
        'hola ${RED}monkey$$ hi ${BLUE}blue$$' | 'hola monkey hi blue'
    }

    def "template with partial"() {

        when:
        def result = ANSIColorOutput.colorizeTemplate(template)

        then:
        result.colors.size() == 3
        result.colors.sort()[0].color == BOLD
        result.colors.sort()[0].start == 5
        result.colors.sort()[0].length == -1
        result.colors.sort()[1].color == RED
        result.colors.sort()[1].start == 5
        result.colors.sort()[1].length == 6
        result.colors.sort()[2].color == BLUE
        result.colors.sort()[2].start == 15
        result.colors.sort()[2].length == 4

        result.toString() == plain
        where:
        template                                       | plain
        'hola ${BOLD}%${RED}monkey$$ hi ${BLUE}blue$$' | 'hola monkey hi blue'
    }
    def "template multiline"() {

        when:
        def result = ANSIColorOutput.colorizeTemplate(template)

        then:
        result.colors.sort()[0].color == BLUE
        result.colors.sort()[0].start == 0
        result.colors.sort()[0].length == 4
        result.colors.sort()[1].color == RED
        result.colors.sort()[1].start == 6
        result.colors.sort()[1].length == 30




        result.toString() == plain
        where:
        template                               | plain
        '${blue}hola$$ \n${RED}\nmonkey \nhella oh yah\n\nhi blue$$' | 'hola \n\nmonkey \nhella oh yah\n\nhi blue'
    }
}
