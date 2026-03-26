package org.texttechnologylab.uce.common.models.search.promode;

import junit.framework.TestCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ProQueryParserTest extends TestCase {

    public void testParsesBooleanPrecedence() {
        var ast = new ProQueryParser().parse("Carex | 'Carex L.' & !divulsa");

        assertTrue(ast.root() instanceof ProBinaryNode);
        var root = (ProBinaryNode) ast.root();
        assertEquals(ProBinaryOperator.OR, root.operator());

        assertTrue(root.left() instanceof ProTermNode);
        assertEquals("Carex", ((ProTermNode) root.left()).value());

        assertTrue(root.right() instanceof ProBinaryNode);
        var andNode = (ProBinaryNode) root.right();
        assertEquals(ProBinaryOperator.AND, andNode.operator());

        assertTrue(andNode.left() instanceof ProTermNode);
        var quoted = (ProTermNode) andNode.left();
        assertEquals("Carex L.", quoted.value());
        assertTrue(quoted.quoted());

        assertTrue(andNode.right() instanceof ProUnaryNode);
        var notNode = (ProUnaryNode) andNode.right();
        assertTrue(notNode.operand() instanceof ProTermNode);
        assertEquals("divulsa", ((ProTermNode) notNode.operand()).value());
    }

    public void testParsesGroupedExpression() {
        var ast = new ProQueryParser().parse("alpina & (herbst | sommer)");
        assertTrue(ast.root() instanceof ProBinaryNode);
        var root = (ProBinaryNode) ast.root();
        assertEquals(ProBinaryOperator.AND, root.operator());
        assertTrue(root.right() instanceof ProGroupNode);
    }

    public void testParsesFollowedByDefaultOperator() {
        var ast = new ProQueryParser().parse("april <-> 1902");
        assertTrue(ast.root() instanceof ProBinaryNode);
        var root = (ProBinaryNode) ast.root();
        assertEquals(ProBinaryOperator.FOLLOWED_BY, root.operator());
        assertEquals(1, root.followDistance());
    }

    public void testParsesFollowedByDistanceOperator() {
        var ast = new ProQueryParser().parse("'Carex muricata' <3> divulsa");

        assertTrue(ast.root() instanceof ProBinaryNode);
        var root = (ProBinaryNode) ast.root();
        assertEquals(ProBinaryOperator.FOLLOWED_BY, root.operator());
        assertEquals(3, root.followDistance());

        assertTrue(root.left() instanceof ProTermNode);
        var left = (ProTermNode) root.left();
        assertEquals("Carex muricata", left.value());
        assertTrue(left.quoted());

        assertTrue(root.right() instanceof ProTermNode);
        assertEquals("divulsa", ((ProTermNode) root.right()).value());
    }

    public void testParsesPrefixOperator() {
        var ast = new ProQueryParser().parse("pere:*");
        assertTrue(ast.root() instanceof ProTermNode);
        var root = (ProTermNode) ast.root();
        assertEquals("pere", root.value());
        assertTrue(root.prefixSearch());
    }

    public void testParsesAllTaxonCommands() {
        assertCommand("K::Animalia", "K::", "Animalia");
        assertCommand("P::Tracheophyta", "P::", "Tracheophyta");
        assertCommand("C::Magnoliopsida", "C::", "Magnoliopsida");
        assertCommand("O::Poales", "O::", "Poales");
        assertCommand("F::Cyperaceae", "F::", "Cyperaceae");
        assertCommand("G::Carex", "G::", "Carex");
        assertCommand("S::muricata", "S::", "muricata");
    }

    public void testParsesGeoCommands() {
        assertCommand("LOC::H.CNL", "LOC::", "H.CNL");
        assertCommand("LOC::H", "LOC::", "H");
        assertCommand("R::lng=9;lat=50;r=70000", "R::", "lng=9;lat=50;r=70000");
    }

    public void testParsesTimeCommands() {
        assertCommand("Y::1880", "Y::", "1880");
        assertCommand("M::2", "M::", "2");
        assertCommand("D::31", "D::", "31");
        assertCommand("E::Winter", "E::", "Winter");
        assertCommand("T::1880-1900", "T::", "1880-1900");
    }

    public void testParsesEscapedQuotedValue() {
        var ast = new ProQueryParser().parse("'O\\'Brien'");
        assertTrue(ast.root() instanceof ProTermNode);
        var root = (ProTermNode) ast.root();
        assertEquals("O'Brien", root.value());
        assertTrue(root.quoted());
    }

    public void testQuotedJoinAllowed() {
        var ast = new ProQueryParser().parse("'Bellis perennis'");
        assertTrue(ast.root() instanceof ProTermNode);
        assertTrue(((ProTermNode) ast.root()).quoted());
    }

    public void testUnquotedJoinRejected() {
        expectSyntaxErrorContains("Bellis perennis", "Unexpected token");
    }

    public void testRejectsEmptyPrefix() {
        expectSyntaxErrorContains(":*", "Prefix search ':*' requires a term");
    }

    public void testRejectsUnclosedQuote() {
        expectSyntaxErrorContains("'Bellis perennis", "Unclosed quote");
    }

    public void testRejectsInvalidFollowedByOperatorText() {
        expectSyntaxErrorContains("april <x> 1902", "Invalid followed-by operator");
    }

    public void testRejectsZeroFollowedByDistance() {
        expectSyntaxErrorContains("april <0> 1902", "must be > 0");
    }

    public void testRejectsUnbalancedGroup() {
        expectSyntaxErrorContains("(alpina & herbst", "Expected ')' to close group");
    }

    public void testRejectsCommandWithoutValue() {
        expectSyntaxErrorContains("K::", "requires a value");
    }

    public void testRejectsWhitespaceInsideCommandValue() {
        expectSyntaxErrorContains("S::Carex muricata", "Unexpected token");
    }

    public void testRejectsInvalidRadiusCommand() {
        expectSyntaxErrorContains("R::lng=8;lat=50", "R:: command must use format");
    }

    public void testRejectsInvalidTimeRangeCommand() {
        expectSyntaxErrorContains("T::1880/1900", "T:: command must use format");
    }

    public void testParsesMixedFullSyntaxSample() {
        var query = "(K::Animalia & LOC::H.CNL) | (!'Bellis perennis' & pere:* <10> T::1880-1900)";
        var ast = new ProQueryParser().parse(query);
        assertTrue(ast.root() instanceof ProBinaryNode);
    }

    public void testParsesPromodequeryFixtureFile() throws IOException {
        var query = Files.readString(resolvePromodeQueryFixture(), StandardCharsets.UTF_8);
        var ast = new ProQueryParser().parse(query);

        assertTrue(ast.root() instanceof ProBinaryNode);
        var root = (ProBinaryNode) ast.root();
        assertEquals(ProBinaryOperator.AND, root.operator());

        assertTrue(root.right() instanceof ProTermNode);
        var right = (ProTermNode) root.right();
        assertEquals("Wasserstufenzeigerwert", right.value());
        assertTrue(right.quoted());
    }

    private void assertCommand(String query, String expectedCommand, String expectedValue) {
        var ast = new ProQueryParser().parse(query);
        assertTrue(ast.root() instanceof ProCommandNode);
        var root = (ProCommandNode) ast.root();
        assertEquals(expectedCommand, root.command());
        assertEquals(expectedValue, root.value());
    }

    private void expectSyntaxErrorContains(String query, String fragment) {
        try {
            new ProQueryParser().parse(query);
            fail("Expected ProModeSyntaxException for query: " + query);
        } catch (ProModeSyntaxException ex) {
            assertTrue("Expected message to contain '" + fragment + "' but got: " + ex.getMessage(),
                    ex.getMessage().contains(fragment));
        }
    }

    private Path resolvePromodeQueryFixture() {
        var candidates = List.of(
                Path.of(".dev/promodequery.txt"),
                Path.of("../.dev/promodequery.txt"),
                Path.of("../../.dev/promodequery.txt")
        );
        for (var candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        fail("Could not resolve .dev/promodequery.txt from current working directory");
        return null;
    }
}
