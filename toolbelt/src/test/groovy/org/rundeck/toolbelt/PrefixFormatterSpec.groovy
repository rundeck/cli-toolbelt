package org.rundeck.toolbelt

import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 2/2/17
 */
class PrefixFormatterSpec extends Specification {
    @Unroll
    def "format"() {
        given:
        def sut = new PrefixFormatter(prefix)
        sut.truncateFinalNewline = truncate

        when:
        def result = sut.format(input)

        then:
        result == expected


        where:
        prefix | input              | truncate | expected
        ''     | 'abc123'           | false    | 'abc123'
        '# '   | 'abc123'           | false    | '# abc123'
        ''     | 'abc123\ndef456'   | false    | 'abc123\ndef456'
        '# '   | 'abc123\ndef456'   | false    | '# abc123\n# def456'
        ''     | 'abc123\ndef456\n' | false    | 'abc123\ndef456\n'
        '# '   | 'abc123\ndef456\n' | false    | '# abc123\n# def456\n'
        '# '   | 'abc\n\n\n'        | false    | '# abc\n# \n# \n'
        ''     | 'abc123\ndef456'   | true     | 'abc123\ndef456'
        '# '   | 'abc123\ndef456'   | true     | '# abc123\n# def456'
        ''     | 'abc123\ndef456\n' | true     | 'abc123\ndef456'
        '# '   | 'abc123\ndef456\n' | true     | '# abc123\n# def456'
        '# '   | 'abc\n\n\n'        | true     | '# abc\n# \n# '
    }
}
