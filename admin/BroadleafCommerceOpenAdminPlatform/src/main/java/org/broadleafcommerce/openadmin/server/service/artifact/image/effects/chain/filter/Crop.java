package org.broadleafcommerce.openadmin.server.service.artifact.image.effects.chain.filter;

import org.broadleafcommerce.openadmin.server.service.artifact.image.Operation;
import org.broadleafcommerce.openadmin.server.service.artifact.image.effects.chain.UnmarshalledParameter;
import org.broadleafcommerce.openadmin.server.service.artifact.image.effects.chain.conversion.ParameterTypeEnum;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.InputStream;
import java.util.Map;

public class Crop extends BaseFilter {

	private RenderingHints hints;
	private Rectangle region;

    public Crop() {
        //do nothing
    }

	public Crop(Rectangle region, RenderingHints hints) {
		this.hints = hints;
		this.region = region;
	}

    @Override
    public Operation buildOperation(Map<String, String[]> parameterMap, InputStream artifactStream, String mimeType) {
        String key = FilterTypeEnum.CROP.toString();
        if (parameterMap.containsKey("filterType") && key.equals(parameterMap.get("filterType")[0])) {
            Operation operation = new Operation();
            operation.setName(key);
            String[] factor = parameterMap.get(key + "-factor");
            operation.setFactor(factor==null?null:Double.valueOf(factor[0]));

            UnmarshalledParameter rectangle = new UnmarshalledParameter();
            String[] rectangleApplyFactor = parameterMap.get(key + "-apply-factor");
            rectangle.setApplyFactor(rectangleApplyFactor == null ? false : Boolean.valueOf(rectangleApplyFactor[0]));
            rectangle.setName("rectangle");
            rectangle.setType(ParameterTypeEnum.RECTANGLE.toString());
            StringBuffer sb = new StringBuffer();
            sb.append(parameterMap.get(key + "-x-amount")[0]);
            sb.append(",");
            sb.append(parameterMap.get(key + "-y-amount")[0]);
            sb.append(",");
            sb.append(parameterMap.get(key + "-width-amount")[0]);
            sb.append(",");
            sb.append(parameterMap.get(key + "-height-amount")[0]);
            rectangle.setValue(sb.toString());

            operation.setParameters(new UnmarshalledParameter[]{rectangle});
            return operation;
        }

        return null;
    }

	/* (non-Javadoc)
	 * @see java.awt.image.BufferedImageOp#filter(java.awt.image.BufferedImage, java.awt.image.BufferedImage)
	 */
	public BufferedImage filter(BufferedImage src, BufferedImage dst) {
		if (src == null) {
            throw new NullPointerException("src image is null");
        }
        if (src == dst) {
            throw new IllegalArgumentException("src image cannot be the "+
                                               "same as the dst image");
        }
        
        boolean needToConvert = false;
        ColorModel srcCM = src.getColorModel();
        ColorModel dstCM;
        BufferedImage origDst = dst;
        
        if (srcCM instanceof IndexColorModel) {
            IndexColorModel icm = (IndexColorModel) srcCM;
            src = icm.convertToIntDiscrete(src.getRaster(), false);
            srcCM = src.getColorModel();
        }
        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
            dstCM = srcCM;
            origDst = dst;
        }
        else {
            dstCM = dst.getColorModel();
            if (srcCM.getColorSpace().getType() !=
                dstCM.getColorSpace().getType())
            {
                needToConvert = true;
                dst = createCompatibleDestImage(src, null);
                dstCM = dst.getColorModel();
            }
            else if (dstCM instanceof IndexColorModel) {
                dst = createCompatibleDestImage(src, null);
                dstCM = dst.getColorModel();
            }
        }
        
        java.awt.image.CropImageFilter cropfilter = new java.awt.image.CropImageFilter(region.x,region.y,region.width,region.height);
		Image returnImage = Toolkit.getDefaultToolkit().createImage(new java.awt.image.FilteredImageSource(src.getSource(),cropfilter));
		dst = ImageConverter.convertImage(returnImage);
		origDst = dst;

	    if (needToConvert) {
            ColorConvertOp ccop = new ColorConvertOp(hints);
            ccop.filter(dst, origDst);
        }
        else if (origDst != dst) {
            java.awt.Graphics2D g2 = origDst.createGraphics();
	    try {
            g2.drawImage(dst, 0, 0, null);
	    } finally {
	        g2.dispose();
	    }
        }

        return origDst;
	}

}
