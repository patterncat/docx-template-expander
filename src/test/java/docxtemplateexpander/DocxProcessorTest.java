package docxtemplateexpander;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import zipcomparator.ZipComparator;

public class DocxProcessorTest {
	private static final Map<String, String> EMPTY_MAP = ImmutableMap.of();
	private static File tempFolder;
	private static File testTemplate;
	private static File templateEquivalent;
	private File resultDocx;
	
	@BeforeClass
	public static void preparePristineCopy() throws InvalidFormatException, IOException {
		tempFolder = java.nio.file.Files.createTempDirectory(DocxProcessorTest.class.getSimpleName()).toFile();
//		System.out.println(tempFolder.toString());
		URL url = DocxProcessorTest.class.getResource("/template.docx");
		testTemplate = new File(url.getFile());
		testTemplate.setWritable(false);
        XWPFDocument doc = new XWPFDocument(OPCPackage.open(testTemplate));
        
        templateEquivalent = new File(tempFolder, "./templateEquivalent.docx");
        templateEquivalent.createNewFile();
		
        try (FileOutputStream out = new FileOutputStream(templateEquivalent)) {
        	doc.write(out);
        }
        
        templateEquivalent.setWritable(false);
	}
	
	@Before
	public void setup() throws IOException {
		resultDocx = new File(tempFolder, "./result.docx");
		resultDocx.createNewFile();
	}

	@After
	public void teardown() throws IOException {
		resultDocx.setWritable(true);
		resultDocx.delete();
	}

	@Test(expected=NullPointerException.class)
	public void testComplainsIfTemplateIsNull() {
		new DocxProcessor(null);
	}

	@Test(expected=IllegalStateException.class)
	public void testComplainsIfTemplateIsNotReadable() throws IOException, InvalidFormatException {
		
		File unreadable = new File(tempFolder, "./unreadableTemplate.docx");
		Files.copy(testTemplate, unreadable);
		DocxProcessor dp = new DocxProcessor(unreadable);
		unreadable.setReadable(false);
		dp.process(EMPTY_MAP, resultDocx);
	}

	@Test(expected=NullPointerException.class)
	public void testComplainsIfDestinationIsNull() throws IOException, InvalidFormatException {
		DocxProcessor dp = new DocxProcessor(testTemplate);
		dp.process(EMPTY_MAP, null);
	}

	@Test(expected=NullPointerException.class)
	public void testComplainsIfMapIsNull() throws IOException, InvalidFormatException {
		DocxProcessor dp = new DocxProcessor(testTemplate);
		dp.process(null, resultDocx);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testComplainsIfDestinationIsNotWritable() throws IOException, InvalidFormatException {
		DocxProcessor dp = new DocxProcessor(testTemplate);
		resultDocx.setWritable(false);
		dp.process(EMPTY_MAP, resultDocx);
	}

	@Test
	public void testCanCopyAFile() throws IOException, InvalidFormatException {
		DocxProcessor dp = new DocxProcessor(testTemplate);
		resultDocx = new File(tempFolder, "./processedWithougSubstitutions.docx");
		resultDocx.createNewFile();
		dp.process(EMPTY_MAP, resultDocx);
		assertTrue(ZipComparator.equal(resultDocx, templateEquivalent));
	}

	@Test
	public void testCanProcessAFile() throws IOException, InvalidFormatException {
		DocxProcessor dp = new DocxProcessor(testTemplate);
		
		Map<String, String> map = ImmutableMap.of(
				Pattern.quote("{{sub1}}"), "test value 1",
				Pattern.quote("{{sub2}}"), "a_very_long_value: - The quick brown fox jumps over the lazy dog",
				Pattern.quote("{{sub3}}"), "this\nis\na multiline\nvalue"
				);
		resultDocx = new File(tempFolder, "./processed_1.docx");
		resultDocx.createNewFile();
		dp.process(map, resultDocx);
		assertFalse(ZipComparator.equal(resultDocx, templateEquivalent));
	}

}
