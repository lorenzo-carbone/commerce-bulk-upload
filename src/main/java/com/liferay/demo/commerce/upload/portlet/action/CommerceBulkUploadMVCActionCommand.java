package com.liferay.demo.commerce.upload.portlet.action;

import com.liferay.commerce.account.model.CommerceAccount;
import com.liferay.commerce.constants.CommerceWebKeys;
import com.liferay.commerce.context.CommerceContext;
import com.liferay.commerce.inventory.engine.CommerceInventoryEngine;
import com.liferay.commerce.product.catalog.CPCatalogEntry;
import com.liferay.commerce.product.content.util.CPContentHelper;
import com.liferay.commerce.product.model.CPInstance;
import com.liferay.commerce.product.service.CPInstanceLocalService;
import com.liferay.commerce.product.util.CPDefinitionHelper;
import com.liferay.commerce.product.util.CPInstanceHelper;
import com.liferay.demo.commerce.upload.bean.ProductBean;
import com.liferay.demo.commerce.upload.constants.CommerceBulkUploadPortletKeys;
import com.liferay.demo.commerce.upload.constants.MVCCommandNames;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.osgi.web.wab.generator.WabGenerator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lorenzo Carbone
 */
@Component(
    immediate = true, 
    property = {
        "javax.portlet.name=" + CommerceBulkUploadPortletKeys.COMMERCEBULKUPLOAD,
        "mvc.command.name=" + MVCCommandNames.UPLOAD_CSV
    }, 
    service = MVCActionCommand.class
)
public class CommerceBulkUploadMVCActionCommand extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
		long companyId = themeDisplay.getCompanyId();

		UploadPortletRequest uploadRequest = PortalUtil.getUploadPortletRequest(actionRequest);
		InputStream is = uploadRequest.getFileAsStream("csvDataFile");

		CommerceContext commerceContext =
				(CommerceContext)actionRequest.getAttribute(
						CommerceWebKeys.COMMERCE_CONTEXT);
		CommerceAccount commerceAccount = commerceContext.getCommerceAccount();

		String filePath = "products.csv";
		try (FileOutputStream fOut = new FileOutputStream(filePath);) {
			int i;
			while ((i = is.read()) != -1) {
				fOut.write(i);
			}

			File csvFile = new File(filePath);

			if (Validator.isNotNull(csvFile)) {
				if (csvFile.getName().contains(".csv")) {

					List<ProductBean> products = new ArrayList<>();
					List<Long> productsIntances = new ArrayList<>();

					for (CSVRecord csvRecord : _getCSVParser(csvFile)) {
						String sku = csvRecord.get("sku");
						String quantity = csvRecord.get("quantity");

						List<CPInstance> cpInstances = _cpInstanceLocalService.searchCPInstances(companyId, sku,
								WorkflowConstants.STATUS_APPROVED, QueryUtil.ALL_POS,
								QueryUtil.ALL_POS, null).getBaseModels();

						for (CPInstance cpInstance : cpInstances) {

							if(StringUtil.equals(cpInstance.getSku(), sku)){

								CPCatalogEntry cpCatalogEntry =
										_cpDefinitionHelper.getCPCatalogEntry(
												commerceAccount.getCommerceAccountId(),
												commerceContext.getCommerceChannelGroupId(), cpInstance.getCPDefinition().getCPDefinitionId(),
												themeDisplay.getLocale());
								String friendlyURL = _cpContentHelper.getFriendlyURL(cpCatalogEntry, themeDisplay);

								productsIntances.add(cpInstance.getCPInstanceId());

								ProductBean product = new ProductBean();
								product.setName(cpInstance.getCPDefinition().getName());
								product.setSku(sku);
								product.setFriendlyURL(friendlyURL);
								product.setThumbnailSrc(_cpInstanceHelper.getCPInstanceThumbnailSrc(cpInstance.getCPInstanceId()));
								product.setQuantity(Integer.parseInt(quantity));

								int quantityInStock = commerceInventoryEngine.getStockQuantity(companyId, sku);
								product.setQuantityInStock(quantityInStock);
								products.add(product);
							}
						}
					}

					actionRequest.setAttribute("cpInstanceHelper", _cpInstanceHelper);

					actionRequest.setAttribute("products", products);
					actionRequest.setAttribute("productsIntances", productsIntances);
					actionResponse.setRenderParameter(
							"mvcPath", "/products.jsp");

				} else {
					log.error("Uploaded File is not CSV file.Your file name is ----> " + csvFile.getName());
				}

			}
		} catch (Exception e) {
			log.error("Exception in CSV File Reading Process :: ", e);
		}

	}

	private CSVParser _getCSVParser(File csvFile) throws Exception {
		CSVFormat csvFormat = CSVFormat.DEFAULT;
		csvFormat = csvFormat.withFirstRecordAsHeader();
		csvFormat = csvFormat.withIgnoreSurroundingSpaces();
		csvFormat = csvFormat.withNullString(StringPool.BLANK);
		try {
			return CSVParser.parse(
					csvFile, Charset.defaultCharset(), csvFormat);
		}
		catch (IOException ioException) {
			log.error(ioException, ioException);
			throw ioException;
		}
	}

	@Reference
	private CommerceInventoryEngine commerceInventoryEngine;

	@Reference
	private CPInstanceLocalService _cpInstanceLocalService;

	@Reference
	private CPInstanceHelper _cpInstanceHelper;

	@Reference
	private CPContentHelper _cpContentHelper;

	@Reference
	private CPDefinitionHelper _cpDefinitionHelper;

	private static final Log log = LogFactoryUtil.getLog(WabGenerator.class);
}