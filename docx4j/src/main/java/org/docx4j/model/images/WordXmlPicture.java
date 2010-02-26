package org.docx4j.model.images;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.vfs.CacheStrategy;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.impl.StandardFileSystemManager;
import org.apache.log4j.Logger;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.docx4j.convert.out.ConvertUtils;
import org.docx4j.model.structure.DocumentModel;
import org.docx4j.model.structure.PageDimensions;
import org.docx4j.model.structure.SectionWrapper;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPart;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.openpackaging.parts.WordprocessingML.MetafileEmfPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MetafilePart;
import org.docx4j.openpackaging.parts.WordprocessingML.MetafileWmfPart;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage.CxCy;
import org.docx4j.openpackaging.parts.WordprocessingML.MetafileWmfPart.SvgDocument;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.SectPr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.traversal.NodeIterator;

/**
 * Generate HTML/XSLFO from 
 * 
 * Originally from OpenXmlView project.
 * TODO - add Microsoft Public Licence
 * 
 * TODO - integrate with our other image handling stuff
 * 
 * Amended .. can generate HTML element, or XSL FO.
 * 
 * @author dev
 *
 */
public class WordXmlPicture {
	
	protected static Logger log = Logger.getLogger(WordXmlPicture.class);
	
	Document document;
    Node imageElement = null;
    Node linkElement = null;
    
    private MetafilePart metaFile;

    /** Extension function to create an HTML <img> element
     * from "E2.0 images" 
     *      //w:drawing/wp:inline
     *     |//w:drawing/wp:anchor
     * @param wmlPackage
     * @param imageDirPath
     * @param pictureData
     * @param picSize
     * @param picLink
     * @param linkData
     * @return
     */
    public static DocumentFragment createHtmlImgE20(WordprocessingMLPackage wmlPackage,
    		String imageDirPath,
    		NodeIterator pictureData, NodeIterator picSize,
    		NodeIterator picLink, NodeIterator linkData) {

    	WordXmlPicture picture = createWordXmlPictureFromE20( wmlPackage,
        		 imageDirPath, pictureData,  picSize,
        		 picLink,  linkData, true);
    	
    	return getHtmlDocumentFragment(picture);

    }
    
    public static DocumentFragment getHtmlDocumentFragment(WordXmlPicture picture) {
    	
    	DocumentFragment docfrag=null;
    	Document d=null;
    	try {
        	if (picture==null) {
    			log.warn("picture was null!");
            	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();        
    			 try {
    				d = factory.newDocumentBuilder().newDocument();
    			} catch (ParserConfigurationException e1) {
    				// TODO Auto-generated catch block
    				e1.printStackTrace();
    			}
    			Element span = d.createElement("span");
    			span.setAttribute("style", "color:red;");
    			d.appendChild(span);
    			
    			Text err = d.createTextNode( "[null img]" );
    			span.appendChild(err);
    		
        	} else if (picture.metaFile==null) {
				// Usual case    	
			    d = picture.createHtmlImageElement();
			} else if (picture.metaFile instanceof MetafileWmfPart) {
				
				SvgDocument svgdoc = ((MetafileWmfPart)picture.metaFile).toSVG();
				d = svgdoc.getDomDocument();
				
			} else if (picture.metaFile instanceof MetafileEmfPart) {
				
	        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();        
				 d = factory.newDocumentBuilder().newDocument();
				
				//log.info("Document: " + document.getClass().getName() );

				Node span = d.createElement("span");			
				d.appendChild(span);
				
				Text err = d.createTextNode( "[TODO emf image]" );
				span.appendChild(err);
				
			}
		} catch (Exception e) {
			log.error(e);
        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();        
			 try {
				d = factory.newDocumentBuilder().newDocument();
			} catch (ParserConfigurationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Element span = d.createElement("span");
			span.setAttribute("style", "color:red;");
			d.appendChild(span);
			
			Text err = d.createTextNode( e.getMessage() );
			span.appendChild(err);
		}
		docfrag = d.createDocumentFragment();
		docfrag.appendChild(d.getDocumentElement());
		return docfrag;
    }

    /** Extension function to create an XSL FO <fo:external-graphic> element
     * from "E2.0 images" 
     *      //w:drawing/wp:inline
     *     |//w:drawing/wp:anchor
     * @param wmlPackage
     * @param imageDirPath
     * @param pictureData
     * @param picSize
     * @param picLink
     * @param linkData
     * @return
     */
    public static DocumentFragment createXslFoImgE20(WordprocessingMLPackage wmlPackage,
    		String imageDirPath,
    		NodeIterator pictureData, NodeIterator picSize,
    		NodeIterator picLink, NodeIterator linkData) {

    	WordXmlPicture picture = createWordXmlPictureFromE20( wmlPackage,
        		 imageDirPath, pictureData,  picSize,
        		 picLink,  linkData, false);
    	
        Document d = picture.createXslFoImageElement();

		DocumentFragment docfrag = d.createDocumentFragment();
		docfrag.appendChild(d.getDocumentElement());

		return docfrag;
    }
    
    /**
     * @param wmlPackage
     * @param imageDirPath - images won't be saved if this is not set
     * @param pictureData
     * @param picSize
     * @param picLink
     * @param linkData
     * @return
     */
    private static WordXmlPicture createWordXmlPictureFromE20(WordprocessingMLPackage wmlPackage,
    		String imageDirPath,
    		NodeIterator pictureData, NodeIterator picSize,
    		NodeIterator picLink, NodeIterator linkData, boolean targetIsHtml) {
    	
    	    	
    	// incoming objects are org.apache.xml.dtm.ref.DTMNodeIterator 
    	// which implements org.w3c.dom.traversal.NodeIterator
    	    	
    	WordXmlPicture picture = new WordXmlPicture();
    	picture.readStandardAttributes( pictureData.nextNode(), targetIsHtml );
    	
    	Node picSizeNode = picSize.nextNode();
    	if ( picSizeNode!=null ) {
            picture.readSizeAttributes(picSizeNode);    		
    	}

    	Node picLinkNode = picLink.nextNode();
        if (picLinkNode != null)
        {
            String linkRelId = ConvertUtils.getAttributeValueNS(picLinkNode, 
            		"http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id");

            if ( linkRelId!=null && !linkRelId.equals("") ) 
            {
            	Relationship rel = wmlPackage.getMainDocumentPart().getRelationshipsPart().getRelationshipByID(linkRelId);
            	
            	if (rel.getTargetMode() == null
            			|| rel.getTargetMode().equals("Internal") ) {
            		
            		picture.setHlinkReference("TODO - save this object");
            		
            	} else {
                    picture.setHlinkReference( rel.getTarget() );            	
            	}
            }

            picture.readLinkAttributes(picLinkNode);
        }
    	
    	Node linkDataNode = linkData.nextNode();
        if (linkDataNode == null) {
        	log.warn("Couldn't find a:blip!");
        } else {
            String imgRelId = ConvertUtils.getAttributeValueNS(linkDataNode, "http://schemas.openxmlformats.org/officeDocument/2006/relationships", "embed");  // Microsoft code had r:link here

            if (imgRelId!=null && !imgRelId.equals(""))
            {
            	picture.setID(imgRelId);            	
            	Relationship rel = wmlPackage.getMainDocumentPart().getRelationshipsPart().getRelationshipByID(imgRelId);
            	
            	if (rel.getTargetMode() == null
						|| rel.getTargetMode().equals("Internal")) {
            		
            		
            		BinaryPart part = (BinaryPart)wmlPackage.getMainDocumentPart()
						.getRelationshipsPart().getPart(rel);
            		
            		if (part instanceof MetafilePart) {
            			
            			picture.metaFile = (MetafilePart)part;
            			
            		} else {

	            		BinaryPartAbstractImage imagepart = (BinaryPartAbstractImage)part;
						
						String uri = handlePart(imageDirPath, picture, imagepart);
						// Scale it?  Shouldn't be necessary, since Word should
						// be providing the height/width
	//					try {
	//						ImageInfo imageInfo = BinaryPartAbstractImage.getImageInfo(uri);
	//						
	//						List<SectionWrapper> sections = wmlPackage.getDocumentModel().getSections();
	//						PageDimensions page = sections.get(sections.size()-1).getPageDimensions();
	//						
	//						picture.ensureFitsPage(imageInfo, page );
	//					} catch (Exception e) {
	//						e.printStackTrace();
	//					}

            		}
					
				} else { // External
					picture.setSrc(rel.getTarget());
					
					// TODO: handle external metafiles
				}

			}

			// if the relationship isn't found, produce a warning
			// if (String.IsNullOrEmpty(picture.Src))
			// {
			// this.embeddedPicturesDropped++;
			// }
		}

		return picture;
	}

	/**
	 * @param imageDirPath
	 * @param picture
	 * @param part
	 * @return uri for the image we've saved, or null
	 */
	private static String handlePart(String imageDirPath, WordXmlPicture picture,
			Part part) {
		try {

			if (imageDirPath.equals("")) {
				
				// TODO: this isn't going to work for XSL FO!
				// So for XSL FO, you always need an imageDirPath! 

				// <img
				// src="data:image/gif;base64,R0lGODlhEAAOALMAAOazToeHh0tLS/7LZv/0jvb29t/f3//Ub/
				//
				// which is nice, except it doesn't work in IE7,
				// and is limited to 32KB in IE8!

				java.nio.ByteBuffer bb = ((BinaryPart) part)
						.getBuffer();
				bb.clear();
				byte[] bytes = new byte[bb.capacity()];
				bb.get(bytes, 0, bytes.length);
				
				byte[] encoded = Base64.encodeBase64(bytes, true);

				picture
						.setSrc("data:" + part.getContentType()
								+ ";base64,"
								+ (new String(encoded, "UTF-8")));
				
				return null;

			} else {
				// Need to save the image

				// To create directory:
				FileObject folder = getFileSystemManager()
						.resolveFile(imageDirPath);
				if (!folder.exists()) {
					folder.createFolder();
				}

				// Construct a file name from the part name
				String partname = part.getPartName().toString();
				String filename = partname.substring(partname
						.lastIndexOf("/") + 1);
				log.debug("image file name: " + filename);

				FileObject fo = folder.resolveFile(filename);
				if (fo.exists()) {

					log.warn("Overwriting (!) existing file!");

				} else {
					fo.createFile();
				}
				// System.out.println("URL: " +
				// fo.getURL().toExternalForm() );
				// System.out.println("String: " + fo.toString() );

				// Save the file
				OutputStream out = fo.getContent()
						.getOutputStream();
				// instance of org.apache.commons.vfs.provider.DefaultFileContent$FileContentOutputStream
				// which extends MonitorOutputStream
			    // which in turn extends BufferedOutputStream
			    // which in turn extends FilterOutputStream.
				
				String src;
				try {
					java.nio.ByteBuffer bb = ((BinaryPart) part)
							.getBuffer();
					bb.clear();
					byte[] bytes = new byte[bb.capacity()];
					bb.get(bytes, 0, bytes.length);

					out.write(bytes);
					
					// Set the attribute
					src = fixImgSrcURL(fo);
					picture.setSrc(src);
					log.info("Wrote @src='" + src);
					return src;
				} finally {
					try {
						fo.close();
						// That Closes this file, and its content.
						// Closing the content in turn
						// closes any open stream.
						// out.flush() is unnecessary, since 
						// FilterOutputStream's close() does do flush() first.
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}					
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
		}
		return null;
	}

	private static FileSystemManager fileSystemManager;
	private static ReadWriteLock aLock = new ReentrantReadWriteLock(true);

	public static FileSystemManager getFileSystemManager() {
		aLock.readLock().lock();

		try {
			if (fileSystemManager == null) {
				try {
					StandardFileSystemManager fm = new StandardFileSystemManager();
					fm.setCacheStrategy(CacheStrategy.MANUAL);
					fm.init();
					fileSystemManager = fm;
				} catch (Exception exc) {
					throw new RuntimeException(exc);
				}
			}

			return fileSystemManager;
		}
        finally
        {
            aLock.readLock().unlock();
        }
    }
    
	/**
	 * imageDirPath is anything VFSJFileChooser can resolve into a FileObject. 
	 * That's enough for saving the image. In order for a web browser to
	 * display it, the URI Scheme has to be something a web browser can
	 * understand. So at that point, webdav:// will have to become http://, 
	 * and smb:// become file:// ...
	 */
    static String fixImgSrcURL( FileObject fo)
    {   	
    	String itemUrl = null;
		try {
			itemUrl = fo.getURL().toExternalForm();
			log.debug(itemUrl);

			String itemUrlLower = itemUrl.toLowerCase();			
	        if (itemUrlLower.startsWith("http://") 
	        		 || itemUrlLower.startsWith("https://")) {
				return itemUrl;
			} else if (itemUrlLower.startsWith("file://")) {
				// we'll convert file protocol to relative reference
				// if this is html output
				
				if (fo.getParent() == null) {
					return itemUrl;					
				} else if (fo.getParent().getURL().toExternalForm().equalsIgnoreCase(
						    getFileSystemManager().resolveFile(System.getProperty("java.io.tmpdir")).getURL().toExternalForm() )) {
					
					// The image is being stored in the system temp directory,
					// so assume this is a pdf export, and preserve the absolute
					// file path

					// org.apache.commons.vfs.provider.local.LocalFile has a
					// method doIsSameFile, but the point of using FileObject is
					// that it won't necessarily be a local file. 
					
					return itemUrl;						
				} else {
		             // Otherwise, assume it is an html export and return a relative path
					return  fo.getParent().getName().getBaseName() 
								+ "/" + fo.getName().getBaseName();
				}
				
			} else if (itemUrlLower.startsWith("webdav://")) {
				// TODO - convert to http:, dropping username / password
				return itemUrl;
			} 			
	        log.warn("How to handle scheme: " + itemUrl );        
		} catch (FileSystemException e) {
			log.error("Problem fixing Img Src URL", e);
		}		    	
    	return itemUrl;        
    }
    
    /** Extension function to create an <img> element
     * from "E1.0 images"
     *  
     *      //w:pict
     * @param wmlPackage
     * @param imageDirPath
     * @param shape
     * @param imageData
     * @return
     */
    public static DocumentFragment createHtmlImgE10(WordprocessingMLPackage wmlPackage,
    		String imageDirPath,
    		NodeIterator shape, NodeIterator imageData) {
    	

    	WordXmlPicture picture = createWordXmlPictureFromE10( wmlPackage,
        		 imageDirPath,
        		 shape,  imageData, true);
    	
    	return getHtmlDocumentFragment(picture);
    }

    /** Extension function to create an <img> element
     * from "E1.0 images"
     *  
     *      //w:pict
     * @param wmlPackage
     * @param imageDirPath
     * @param shape
     * @param imageData
     * @return
     */
    public static DocumentFragment createXslFoImgE10(WordprocessingMLPackage wmlPackage,
    		String imageDirPath,
    		NodeIterator shape, NodeIterator imageData) {
    	

    	WordXmlPicture picture = createWordXmlPictureFromE10( wmlPackage,
        		 imageDirPath,
        		 shape,  imageData, false);
    	
    	if (picture==null) {
    		
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Document d;
			try {
				d = factory.newDocumentBuilder().newDocument();
	    		return d.createDocumentFragment();
			} catch (ParserConfigurationException e) {
				log.error(e);
				return null;
			}  
			
    	} else {
    	
	        Document d = picture.createXslFoImageElement();
	
			DocumentFragment docfrag = d.createDocumentFragment();
			docfrag.appendChild(d.getDocumentElement());
	
			return docfrag;
    	}
    }
    
    
    private static WordXmlPicture createWordXmlPictureFromE10(WordprocessingMLPackage wmlPackage,
    		String imageDirPath,
    		NodeIterator shape, NodeIterator imageData, boolean targetIsHtml) {
    	
    	// Sanity check; though XSLT should check these nodes are non null
    	// before invoking this extension function.
    	Node shapeNode = null;
    	Node imageDataNode = null;
    	if (shape!=null) {
    		shapeNode = shape.nextNode();
    	}
    	if (imageData!=null) {
    		imageDataNode = imageData.nextNode();
    	}
    	if (shapeNode==null
    			|| imageDataNode ==null ) {
    		log.error("w:pict contains something other than an image?");
    		return null;
    	}
    	// OK
    	
    	WordXmlPicture picture = new WordXmlPicture();
    	picture.readStandardAttributes( shapeNode, targetIsHtml );    	

        String imgRelId = ConvertUtils.getAttributeValueNS(imageDataNode, 
        		"http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id"); 

        if (imgRelId!=null && !imgRelId.equals(""))
        {
        	Relationship rel = wmlPackage.getMainDocumentPart().getRelationshipsPart().getRelationshipByID(imgRelId);
        	
            // if the relationship isn't found, produce a warning
            //if (String.IsNullOrEmpty(picture.Src))
            //{
            //    this.embeddedPicturesDropped++;
            //}
        	
        	if (rel.getTargetMode() == null
        			|| rel.getTargetMode().equals("Internal") ) {
        		
        		BinaryPart part = (BinaryPartAbstractImage)wmlPackage.getMainDocumentPart()
					.getRelationshipsPart().getPart(rel);
        		
        		if (part instanceof MetafilePart) {
        			
        			picture.metaFile = (MetafilePart)part;
        			
        		} else {
        		
            		BinaryPartAbstractImage imagepart = (BinaryPartAbstractImage)part;       			
            		String uri = handlePart(imageDirPath, picture, imagepart);
				
				// Scale it?  Shouldn't be necessary, since Word should
				// be providing the height/width
//				try {
//					ImageInfo imageInfo = BinaryPartAbstractImage.getImageInfo(uri);
//					
//					List<SectionWrapper> sections = wmlPackage.getDocumentModel().getSections();
//					PageDimensions page = sections.get(sections.size()-1).getPageDimensions();
//					
//					picture.ensureFitsPage(imageInfo, page );
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
        		}				
        		
        	} else {
                picture.setSrc( rel.getTarget() );            	
        	}

        }
        
        return picture;
    }  
    
    
//    /**
//     * If the docx does not explicitly size the
//     * image, check that it will fit on the page 
//     */
//    private void ensureFitsPage(ImageInfo imageInfo, PageDimensions page) {
//
//    	
//    	CxCy cxcy = BinaryPartAbstractImage.CxCy.scale(imageInfo, page);    
//    	
//    	if (cxcy.isScaled() ) {
//    		log.info("Scaled to fit page width");
//    		this.setWidth( Math.round(cxcy.getCx()/extentToPixelConversionFactor) );
//    		this.setHeight( Math.round(cxcy.getCy()/extentToPixelConversionFactor) );    
//    		// That gives pixels, which is ok for HTML, but for XSL FO, we want pt or mm etc
//    	}
//    	
//    }
	
	void setAttribute(String name, String value) {
		
		setAttribute( imageElement, name, value );
		
	}
	void setAttribute(Node element, String name, String value) {
		
    	org.w3c.dom.Attr tmpAtt = document.createAttribute(name);
    	tmpAtt.setValue(value);
    	element.getAttributes().setNamedItem(tmpAtt);
    	
    	log.debug("<" + element.getLocalName() + " @"+ name + "=\"" + value);
		
	}
	
    /// <id guid="100b714f-5397-4420-958b-e03c2d021f7c" />
    /// <owner alias="ROrleth" />
	public Document createHtmlImageElement()
    {

        try {
            // Create a DOM builder and parse the fragment
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            
            document = factory.newDocumentBuilder().newDocument();
            
            
            imageElement = document.createElement("img");

            if (src !=null && !src.equals(""))
            {
            	setAttribute("src", src);
            }

            if (id !=null && !id.equals("") )
            {
                setAttribute("id", id);
            }

            if (alt !=null && !alt.equals("") )
            {
                setAttribute("alt", alt);
            }

            if (style !=null && !style.equals("") )
            {
                setAttribute("style", style);
            }

            if (widthSet)
            {
                setAttribute("width",  Integer.toString(width));
            }

            if (heightSet)
            {
                setAttribute("height", Integer.toString(height));
            }

            if (hlinkRef !=null && !hlinkRef.equals(""))
            {
                linkElement = document.createElement("a");

                setAttribute(linkElement, "href", hlinkRef);

                if (targetFrame !=null && !targetFrame.equals(""))
                {
                    setAttribute(linkElement, "target", targetFrame);
                }

                if (tooltip !=null && !tooltip.equals(""))
                {
                    setAttribute(linkElement, "title", tooltip);
                }

                linkElement.appendChild(imageElement);

                imageElement = linkElement;
            }
            
            document.appendChild(imageElement);
            
            return document;
            
        } catch (Exception e) {
        	log.error(e);
            return null;
        }
        
    }

	private Document createXslFoImageElement()
    {

        try {
            // Create a DOM builder and parse the fragment
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            
            document = factory.newDocumentBuilder().newDocument();
                        
            imageElement = document.createElementNS("http://www.w3.org/1999/XSL/Format", 
			"fo:external-graphic"); 	

            if (src !=null && !src.equals(""))
            {
            	setAttribute("src", src);
            }

//            if (id !=null && !id.equals("") )
//            {
//                setAttribute("id", id);
//            }
//
//            if (alt !=null && !alt.equals("") )
//            {
//                setAttribute("alt", alt);
//            }
//
//            if (style !=null && !style.equals("") )
//            {
//                setAttribute("style", style);
//            }
//
            if (widthSet)
            {
                setAttribute("content-width",  Integer.toString(width)+units);
            }

            if (heightSet)
            {
                setAttribute("content-height", Integer.toString(height)+units);
            }
//
//            if (hlinkRef !=null && !hlinkRef.equals(""))
//            {
//                linkElement = document.createElement("a");
//
//                setAttribute(linkElement, "href", hlinkRef);
//
//                if (targetFrame !=null && !targetFrame.equals(""))
//                {
//                    setAttribute(linkElement, "target", targetFrame);
//                }
//
//                if (tooltip !=null && !tooltip.equals(""))
//                {
//                    setAttribute(linkElement, "title", tooltip);
//                }
//
//                linkElement.appendChild(imageElement);
//
//                imageElement = linkElement;
//            }
            
            document.appendChild(imageElement);
            
            return document;
            
        } catch (Exception e) {
        	log.error(e);
            return null;
        }
        
    }
	
	String units = "";
	
    /// <id guid="233b126d-66d0-476e-bcd1-ce30bdc3e65b" />
    /// <owner alias="ROrleth" />
    public void readStandardAttributes(Node fromNode, boolean targetIsHtml)
    {
        this.id = ConvertUtils.getAttributeValue(fromNode, "id");
        this.pType = ConvertUtils.getAttributeValue(fromNode, "type");
        this.alt = ConvertUtils.getAttributeValue(fromNode, "alt");
        this.style = ConvertUtils.getAttributeValue(fromNode, "style");

        // E10: <v:shape style="width:428.25pt;height:321pt"
        // hmm, don't want a whole CSS parser just for this..
        // But if we did, it would be something like
		// CSSStyleDeclaration cssStyleDeclaration = = cssOMParser.parseStyleDeclaration(
		//			new org.w3c.css.sac.InputSource(new StringReader(styleVal)) );

            if (style.lastIndexOf("width")>=0) {
            	setWidth( getStyleVal("width",targetIsHtml));
            }
            if (style.lastIndexOf("height")>=0) {
            	setHeight( getStyleVal("height",targetIsHtml));
            }
    }
    
    private int getStyleVal(String name, boolean targetIsHtml) {
    	
    	// Assumptions: 1, the named attribute is present
    	//if (style.lastIndexOf(name)<0) return 0;
    	
    	// Assumptions: 2, the dimension is given in pt 
    	
        // E10: <v:shape style="width:428.25pt;height:321pt"
    	log.debug(style);

    	int beginIndex = style.indexOf(name) + name.length()+1; // +1 for the ':'
    	int endIndex = style.indexOf("pt", beginIndex);
    	
    	String val = style.substring(beginIndex, endIndex);
    	
    	
    	float f = Float.parseFloat(val);
    	
    	// 72 points per inch
    	// so, assuming 72 pdi, there is 1 point per pixel
    	// so no further conversion is necessary
    	// All we need to do is set the units for XSL FO
    	if (!targetIsHtml) {
    		units="pt";	
    	}
    	
    	return Math.round(f);
    	
    	
    }
    

    /// <id guid="048da999-6fbe-41b9-9639-de0e084f3da3" />
    /// <owner alias="ROrleth" />
    public void readLinkAttributes(Node fromNode)
    {
        this.targetFrame = ConvertUtils.getAttributeValue(fromNode, "tgtFrame");
        this.tooltip = ConvertUtils.getAttributeValue(fromNode, "tooltip");
    }

    private final int extentToPixelConversionFactor = 12700;

    /// <id guid="cb8dfd67-57bb-4ebc-af9d-f6062d25b9ba" />
    /// <owner alias="ROrleth" />
    public void readSizeAttributes(Node fromNode)
    {
        String temp = null;
        temp = ConvertUtils.getAttributeValue(fromNode, "cx");
        if (temp !=null && !temp.equals("") )
        {
            setWidth ( Integer.parseInt(temp) / extentToPixelConversionFactor );
            	
            	//Convert.ToUInt32(temp, CultureInfo.InvariantCulture) / ExtentToPixelConversionFactor;
        }
        temp = ConvertUtils.getAttributeValue(fromNode, "cy");
        if (temp !=null && !temp.equals("") )
        {
            setHeight( Integer.parseInt(temp) / extentToPixelConversionFactor );
//            this.height = Convert.ToUInt32(temp, CultureInfo.InvariantCulture) / ExtentToPixelConversionFactor;
        }

    }

    private int width;
    private boolean widthSet;

    /// <summary>
    /// Width in pixels
    /// </summary>
    /// <id guid="f01d0577-7f05-4c1b-8dcf-ad36b93bbc3c" />
    /// <owner alias="ROrleth" />
    public int getWidth() {
    	return this.width;
    }
    public void setWidth(int value) {
		this.widthSet = true;
		this.width = value;
	}

    // / <summary>
    // / WidthSet - returns true if the width has been set intentionally
    /// </summary>
    /// <id guid="d3cb7ab3-a36a-455f-90aa-539904f2781e" />
    /// <owner alias="ROrleth" />
    public boolean getWidthSet() 
    {
    	return this.widthSet;
    }

    private int height;
    private boolean heightSet;

    /// <summary>
    /// Height in pixels
    /// </summary>
    /// <id guid="8a499702-53a6-430d-b0d1-7ef10e7711f1" />
    /// <owner alias="ROrleth" />
    public int getHeight() {
		return this.height;
	}

	public void setHeight(int value) {
		this.heightSet = true;
		this.height = value;
	}

	// / <summary>
	// / HeightSet - returns true if the height has been set intentionally
	// / </summary>
	// / <id guid="ad9b2c47-ce49-4104-97c1-8fe130d40fcd" />
	// / <owner alias="ROrleth" />
	public boolean getHeightSet() {
		return this.heightSet;
	}

	private String targetFrame;

	// / <summary>
	// / Target frame property
	// / </summary>
	// / <id guid="1acdad51-bba9-4876-9c53-b0753094c3e9" />
	// / <owner alias="ROrleth" />
	public String getTargetFrame() {
		return this.targetFrame;
	}

	public void setTargetFrame(String value) {
		this.targetFrame = value;
	}

    private String tooltip;

    // / <summary>
    // / tooltip property
    // / </summary>
    // / <id guid="c7b612aa-9970-49be-9569-44c62e4d1aa5" />
    // / <owner alias="ROrleth" />
    public String getTooltip() {
		return this.tooltip;
	}

	public void setTooltip(String value) {
		this.tooltip = value;
	}

	private String hlinkRef;

	// / <summary>
	// / store the hyperlink that the picture points to, if applicable
	// / </summary>
	// / <id guid="862f74dc-b0a2-44b9-8d0c-4c6d78abaeca" />
	// / <owner alias="ROrleth" />
	public String getHlinkReference() {
		return this.hlinkRef;
	}

	public void setHlinkReference(String value) {
		this.hlinkRef = value;
	}

    private String alt;
    // / <summary>
    // / The attribute of the v:shape node which maps to the
    // / 'alt' attribute of and HTML 'img' tag.
    // / </summary>
    // / <remarks>
    /// Also known as the 'alternate text' property of an
    /// HTML image.
    /// </remarks>
    /// <value>
    /// </value>
    /// <id guid="712de8d5-b603-4c01-a231-183c9de68db5" />
    /// <owner alias="ROrleth" />
    public String getAlt() {
		return this.alt;
	}

	public void setAlt(String value) {
		this.alt = value;
	}

    private byte[] data;
    // / <summary>
    // / The decoded data from the corresponding 'w:bindata'
    /// node of the Word Document.
    /// </summary>
    /// <remarks>
    /// This property is set by the conversion process.
    /// </remarks>
    /// <value>
    /// </value>
    /// <id guid="130108bf-d980-4753-b674-4d489acf485c" />
    /// <owner alias="ROrleth" />
    public byte[] getData() {
		return this.data;
	}

	public void setData(byte[] value) {
		this.data = value;
	}

	private String id;

	// / <summary>
	// / The identifier of the picture unique only within the scope of
	// / the Word Document.
	// / </summary>
	// / <value>
	// / </value>
	// / <id guid="e0d6cf93-79f7-4a38-884c-6b494b244664" />
	// / <owner alias="ROrleth" />
	public String getID() {
		return this.id;
	}

	public void setID(String value) {
		this.id = value;
	}

    private String src;
    public String getSrc() {
		return this.src;
	}
	public void setSrc(String value) {
		this.src = value;
	}

	
	
    private String style;
    // / <summary>
    // / The attribute of the v:shape node which maps to the
    /// 'style' attribute of and HTML 'img' tag.
    /// </summary>
    /// <value>
    /// </value>
    /// <id guid="700b62da-d914-4a40-aa96-1437d2b314e1" />
    /// <owner alias="ROrleth" />
    public String getStyle() {
		return this.style;
	}

	public void setStyle(String value) {
		this.style = value;
	}

    private String pType;
    // / <summary>
    // / The type of the picture as specified by the attribute of the
	// v:shape node
    /// within the Word Document.
    /// </summary>
    /// <remarks>
    /// This value is used as an identifier for a v:type node, which used to specify
    /// properties of the picture within the Word Document.
    /// </remarks>
    /// <value>
    /// </value>
    /// <id guid="78bf5c95-1d55-423c-bc34-92d926203e83" />
    /// <owner alias="ROrleth" />
    public String getPType() {
		return this.pType;
	}

	public void setPType(String value) {
		this.pType = value;
	}
}
