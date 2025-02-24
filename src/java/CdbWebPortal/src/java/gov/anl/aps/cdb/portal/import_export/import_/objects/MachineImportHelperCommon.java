/*
 * Copyright (c) UChicago Argonne, LLC. All rights reserved.
 * See LICENSE file.
 */
package gov.anl.aps.cdb.portal.import_export.import_.objects;

import gov.anl.aps.cdb.common.exceptions.CdbException;
import gov.anl.aps.cdb.portal.controllers.CdbEntityController;
import gov.anl.aps.cdb.portal.controllers.ItemDomainCatalogController;
import gov.anl.aps.cdb.portal.controllers.ItemDomainInventoryController;
import gov.anl.aps.cdb.portal.controllers.ItemDomainMachineDesignController;
import gov.anl.aps.cdb.portal.controllers.utilities.ItemDomainMachineDesignControllerUtility;
import gov.anl.aps.cdb.portal.import_export.import_.objects.handlers.LocationHandler;
import gov.anl.aps.cdb.portal.import_export.import_.objects.specs.BooleanColumnSpec;
import gov.anl.aps.cdb.portal.import_export.import_.objects.specs.ColumnSpec;
import gov.anl.aps.cdb.portal.import_export.import_.objects.specs.CustomColumnSpec;
import gov.anl.aps.cdb.portal.import_export.import_.objects.specs.FloatColumnSpec;
import gov.anl.aps.cdb.portal.import_export.import_.objects.specs.MachineItemRefColumnSpec;
import gov.anl.aps.cdb.portal.import_export.import_.objects.specs.MultiDomainRefColumnSpec;
import gov.anl.aps.cdb.portal.import_export.import_.objects.specs.NameHierarchyColumnSpec;
import gov.anl.aps.cdb.portal.import_export.import_.objects.specs.StringColumnSpec;
import gov.anl.aps.cdb.portal.model.db.beans.ItemDomainMachineDesignFacade;
import gov.anl.aps.cdb.portal.model.db.entities.Item;
import gov.anl.aps.cdb.portal.model.db.entities.ItemDomainInventory;
import gov.anl.aps.cdb.portal.model.db.entities.ItemDomainMachineDesign;
import gov.anl.aps.cdb.portal.model.db.entities.UserInfo;
import gov.anl.aps.cdb.portal.view.objects.KeyValueObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author craig
 */
public class MachineImportHelperCommon {
    
    public static final String KEY_NAME = "name";
    public static final String KEY_INDENT = "indentLevel";
    public static final String KEY_MD_ITEM = "importMdItem";
    public static final String KEY_SORT_ORDER = "importSortOrder";
    public static final String KEY_ASSIGNED_ITEM = "importAssignedItemString";
    public static final String KEY_ASSEMBLY_PART = "importAssemblyPart";
    public static final String KEY_INSTALLED = "importIsInstalled";
    public static final String KEY_LOCATION = "location";
    public static final String KEY_IS_TEMPLATE = "importIsTemplateString";
    public static final String KEY_TEMPLATE_INVOCATION = "importTemplateAndParameters";

    public static final String HEADER_PARENT = "Parent Item";
    public static final String HEADER_PATH = "Parent Path";
    public static final String HEADER_BASE_LEVEL = "Level";
    public static final String HEADER_NAME = "Name";
    public static final String HEADER_ALT_NAME = "Alternate Name";
    public static final String HEADER_DESCRIPTION = "Description";
    public static final String HEADER_SORT_ORDER = "Sort Order";
    public static final String HEADER_ASSIGNED_ITEM = "Assigned Catalog/Inventory Item";
    public static final String HEADER_ASSEMBLY_PART = "Assembly Part";
    public static final String HEADER_INSTALLED = "Is Installed";
    public static final String HEADER_LOCATION = "Location";
    public static final String HEADER_TEMPLATE = "Is Template?";
    public static final String HEADER_TEMPLATE_INVOCATION = "Template Instantiation";
    
    public static final String OPTION_EXPORT_NUM_LEVELS = "Number of Hierarchy Levels (optional, leave blank to export full hierarchy)";
    
    private String optionImportRootItemName = null;
    private ItemDomainMachineDesign rootItem = null;
    
    private String optionExportNumLevels = null;

    public static MachineItemRefColumnSpec existingMachineItemColumnSpec(
            List<ColumnModeOptions> options, 
            ItemDomainMachineDesign rootItem, 
            String columnLabel, 
            String domainProperty,
            String domainSetter,
            String domainExportGetter) {
        
        // default to parent item column header but allow override
        String label;
        if (columnLabel != null) {
            label = columnLabel;
        } else {
            label = HEADER_PARENT;
        }
        
        String propertyName;
        if (domainProperty != null) {
            propertyName = domainProperty;
        } else {
            propertyName = KEY_MD_ITEM;
        }
        
        String setter;
        if (domainSetter != null) {
            setter = domainSetter;
        } else {
            setter = "setImportMdItem";
        }
        
        String exportGetter = null;
        if (domainExportGetter != null) {
            exportGetter = domainExportGetter;
        }
        
        return new MachineItemRefColumnSpec(
                label,
                propertyName,
                setter,
                "CDB ID, name, or path of parent machine design item. Name must be unique and prefixed with '#'. Path must be prefixed with '#', start with a '/', and use '/' as a delimiter. If name includes an embedded '/' character, escape it by preceding with a '\' character.",
                exportGetter,
                options,
                rootItem);
    }
    
    public static NameHierarchyColumnSpec nameHierarchyColumnSpec(List<ColumnModeOptions> options) {
        return new NameHierarchyColumnSpec(
                "Name hierarchy indent level column, specifies name of item to create within parent at previous indent level. Entries in first level column are created as child of specified parent item, or as top level elements when no parent is specified.",
                options,
                HEADER_BASE_LEVEL,
                KEY_NAME,
                KEY_INDENT,
                3);
    }
    
    public static StringColumnSpec parentPathColumnSpec(List<ColumnModeOptions> options) {
        return new StringColumnSpec(
                HEADER_PATH, 
                "importParentPath", 
                "setImportParentPath", 
                "Parent path, for documentation purposes only, ignored on update.", 
                "getImportParentPath", 
                options, 
                0);
    }

    public static StringColumnSpec nameColumnSpec(List<ColumnModeOptions> options) {
        return new StringColumnSpec(
                HEADER_NAME,
                "name",
                "setName",
                "Machine element name.",
                "getName",
                options,
                128);
    }
    
    public static StringColumnSpec altNameColumnSpec(List<ColumnModeOptions> options) {
        return new StringColumnSpec(
                HEADER_ALT_NAME,
                "alternateName",
                "setAlternateName",
                "Machine element alternate name.",
                "getAlternateName",
                options,
                128);
    }
    
    public static StringColumnSpec descriptionColumnSpec(List<ColumnModeOptions> options) {
        return new StringColumnSpec(
                HEADER_DESCRIPTION,
                "description",
                "setDescription",
                "Machine element description.",
                "getDescription",
                options,
                256);
    }
    
    public static FloatColumnSpec sortOrderColumnSpec(List<ColumnModeOptions> options) {
        return new FloatColumnSpec(
                HEADER_SORT_ORDER,
                KEY_SORT_ORDER,
                "setImportSortOrder",
                "Sort order within parent item (as decimal), defaults to order in input sheet when creating new items.",
                "getExportSortOrder",
                ColumnSpec.BLANK_COLUMN_EXPORT_METHOD,
                options);
    }
    
    public static MultiDomainRefColumnSpec assignedItemColumnSpec(List<ColumnModeOptions> options) {
        List<CdbEntityController> controllers = new ArrayList<>();
        controllers.add(ItemDomainCatalogController.getInstance());
        controllers.add(ItemDomainInventoryController.getInstance());
        return new MultiDomainRefColumnSpec(
                HEADER_ASSIGNED_ITEM,
                KEY_ASSIGNED_ITEM,
                "setImportAssignedItem",
                "CDB ID, QR ID or name of assigned catalog or inventory item. Name can only be used for catalog items and must be unique and prefixed with '#'. QR ID must be prefixed with 'qr:'.",
                "getAssignedItem",
                "getCatalogItemAttributeMap",
                options,
                controllers,
                Item.class,
                "",
                false,
                true,
                false);
    }

    public static StringColumnSpec assemblyPartColumnSpec(List<ColumnModeOptions> options) {
        return new StringColumnSpec(
                HEADER_ASSEMBLY_PART,
                KEY_ASSEMBLY_PART,
                "setImportAssemblyPart",
                "Assembly part name for this child. Child and parent item most both specify the same assembly catalog item in column H.",
                "getImportAssemblyPart",
                options,
                0);
    }
    
    public static BooleanColumnSpec isInstalledColumnSpec(List<ColumnModeOptions> options) {
        return new BooleanColumnSpec(
                HEADER_INSTALLED,
                KEY_INSTALLED,
                "setImportIsInstalled",
                "Specify true/false for item with assigned inventory. Leave blank otherwise.",
                "getImportIsInstalled",
                options);
    }

    public static BooleanColumnSpec isTemplateColumnSpec(List<ColumnModeOptions> options) {
        return new BooleanColumnSpec(
                HEADER_TEMPLATE,
                KEY_IS_TEMPLATE,
                "setImportIsTemplate",
                "True/yes if item is template, false/no otherwise.",
                "getIsItemTemplate",
                options);
    }
    
    public static StringColumnSpec templateInvocationColumnSpec(List<ColumnModeOptions> options) {
        return new StringColumnSpec(
                HEADER_TEMPLATE_INVOCATION,
                KEY_TEMPLATE_INVOCATION,
                "setImportTemplateAndParameters",
                "Template to instantiate with required parameters, e.g., 'PS-SR-S{nn}-CAB1(nn=24)'.",
                "getImportTemplateAndParameters",
                options,
                0);
    }
    
    public static HelperWizardOption optionImportRootMachineItem() {
        return new HelperWizardOption(
                "Default Root Machine Item (optional)",
                "Optional root of machine hierarchy to locate items within, used to locate items by name without specifying full path.",
                "optionImportRootItemName",
                HelperOptionType.STRING,
                ImportMode.CREATE);
    }

    public String getOptionImportRootItemName() {
        return optionImportRootItemName;
    }

    public void setOptionImportRootItemName(String optionImportRootItemName) {
        this.optionImportRootItemName = optionImportRootItemName;
    }
    
    public ItemDomainMachineDesign getRootItem() {
        return rootItem;
    }
    
    public ValidInfo validateOptionImportRootItemName() {

        boolean isValid = true;
        String validString = "";

        if (optionImportRootItemName != null) {
            if ((!optionImportRootItemName.isEmpty())) {
                List<ItemDomainMachineDesign> topLevelMatches = new ArrayList<>();
                List<ItemDomainMachineDesign> matchingItems
                        = ItemDomainMachineDesignFacade.getInstance().findByName(optionImportRootItemName);
                for (ItemDomainMachineDesign item : matchingItems) {
                    if (item.getParentMachineDesign() == null) {
                        // top-level item matches name
                        topLevelMatches.add(item);
                    }
                }

                if (topLevelMatches.size() == 1) {
                    rootItem = topLevelMatches.get(0);
                } else if (topLevelMatches.size() == 0) {
                    isValid = false;
                    validString = "no matching top-level machine element with name: " + optionImportRootItemName;
                } else {
                    // more than one match
                    isValid = false;
                    validString = "multiple matching top-level machine elements with name: " + optionImportRootItemName;
                }
            } else {
                // null out option if empty string
                optionImportRootItemName = null;
            }
        }

        return new ValidInfo(isValid, validString);
    }

    public static HelperWizardOption optionExportNumLevels() {
        return new HelperWizardOption(
                OPTION_EXPORT_NUM_LEVELS,
                "Number of hierarchy levels to export, starting from top-level.",
                "optionExportNumLevels",
                HelperOptionType.INTEGER,
                null);
    }

    public String getOptionExportNumLevels() {
        return optionExportNumLevels;
    }

    public void setOptionExportNumLevels(String numLevels) {
        this.optionExportNumLevels = numLevels;
    }
    
    public void reset() {
        rootItem = null;
        optionImportRootItemName = null;
        optionExportNumLevels = null;
    }
    
    private ItemDomainMachineDesign newInvalidUpdateInstance() {
        return ItemDomainMachineDesignController.getInstance().createEntityInstance();
    }
    
    public CreateInfo retrieveEntityInstance(Map<String, Object> rowMap) {

        boolean isValid = true;
        String validString = "";

        ItemDomainMachineDesign existingItem = (ItemDomainMachineDesign) rowMap.get(KEY_MD_ITEM);
        if (existingItem == null) {
            existingItem = newInvalidUpdateInstance();
            isValid = false;
            validString = "Unable to find existing machine item with specified ID or name.";
        }

        return new CreateInfo(existingItem, isValid, validString);
    }

    public static TemplateInvocationInfo loadTemplateAndSetParamValues(Map<String, Object> rowMap) {
        
        boolean isValid = true;
        String validString = "";
        ItemDomainMachineDesign templateItem = null;
        List<KeyValueObject> varNameList = null;
        
        String templateName = "";
        Map<String, String> varNameValueMap = new HashMap<>();
        ItemDomainMachineDesignController controller = ItemDomainMachineDesignController.getInstance();
        ItemDomainMachineDesignControllerUtility utility = new ItemDomainMachineDesignControllerUtility();
        
        String templateParams = (String) rowMap.get(MachineImportHelperCommon.KEY_TEMPLATE_INVOCATION);
        if ((templateParams == null) || (templateParams.isEmpty())) {
            isValid = false;
            validString = "No template/parameters specified.";
        } else {

            // check that cell value matches pattern, basically of the form "*(*)"
            String templateRegex = "[^\\(]*\\([^\\)]*\\)";
            if (!templateParams.matches(templateRegex)) {
                // invalid cell value doesn't match pattern
                isValid = false;
                validString = "invalid format for template and parameters: " + templateParams;
            } else {

                int indexOpenParen = templateParams.indexOf('(');
                int indexCloseParen = templateParams.indexOf(')');

                // parse and validate template name
                templateName = templateParams.substring(0, indexOpenParen);
                if ((templateName == null) || (templateName.isEmpty())) {
                    // unspecified template name
                    isValid = false;
                    validString = "template name not provided: " + templateParams;
                } else {

                    // parse and validate variable string
                    String templateVarString = templateParams.substring(indexOpenParen + 1, indexCloseParen);
                    varNameValueMap = new HashMap<>();
                    // iterate through comma separated list of "varName=value" pairs
                    String[] varArray = templateVarString.split(",");
                    for (String varNameValue : varArray) {
                        String[] nameValueArray = varNameValue.split("=");
                        if (nameValueArray.length != 2) {
                            // invalid format
                            isValid = false;
                            validString = "invalid format for template parameters: " + templateVarString;
                        } else {

                            String varName = nameValueArray[0].strip();
                            String varValue = nameValueArray[1].strip();
                            varNameValueMap.put(varName, varValue);
                        }
                    }
                }
            }
        }
        
        if (!isValid) {
            return new TemplateInvocationInfo(null, null, isValid, validString);
            
        } else {
            
            // retrieve specified template            
            try {
                templateItem = ItemDomainMachineDesignFacade.getInstance().findUniqueTemplateByName(templateName);
            } catch (CdbException ex) {
                isValid = false;
                validString
                        = "exception retrieving specified template, possibly non-unique name: "
                        + templateName + ": " + ex;
            }

            if ((isValid) && (templateItem == null)) {
                isValid = false;
                validString = "no template found for name: " + templateName;
                
            } else {

                // check that it's a template
                if (!templateItem.getIsItemTemplate()) {
                    isValid = false;
                    validString = "specified template name is not a template: " + templateName;
                    
                } else {
                    // generate list of variable name/value pairs
                    controller.setMachineDesignNameList(new ArrayList<>());
                    varNameList = utility.generateMachineDesignTemplateNameListRecursivelly(templateItem);
                    for (KeyValueObject obj : varNameList) {
                        
                        // check that all params in template are specified in import params
                        if (!varNameValueMap.containsKey(obj.getKey())) {
                            // import params do not include a param specified for template
                            isValid = false;
                            validString = "specified template parameters missing required variable: " + obj.getKey();
                            
                        } else {
                            obj.setValue(varNameValueMap.get(obj.getKey()));
                        }
                    }
                }
            }

        }
        
        return new TemplateInvocationInfo(templateItem, varNameList, isValid, validString);
    }
    
    public static ValidInfo handleAssignedItem(
            ItemDomainMachineDesign item, 
            Item assignedItem, 
            String assemblyPartName,
            UserInfo user, 
            Boolean isInstalled) {
        
        boolean isValid = true;
        String validString = "";
        
        Boolean itemIsTemplate = item.getIsItemTemplate();

        // template items cannot have assigned inventory - only catalog
        if (itemIsTemplate && ((assignedItem instanceof ItemDomainInventory))) {
            isValid = false;
            validString = "Template cannot have assigned inventory item, must use catalog item";
            return new ValidInfo(isValid, validString);
        }
        
        // validate "is installed" column value
        if (isInstalled != null) {

            // can't specify isInstalled for template
            if (itemIsTemplate) {
                isValid = false;
                validString = "'" + MachineImportHelperCommon.HEADER_INSTALLED
                        + "' cannot be specified for template item";
                return new ValidInfo(isValid, validString);
            }

            // can't set isInstalled if assembly part is specified
            if (assemblyPartName != null && !assemblyPartName.isEmpty()) {
                isValid = false;
                validString = "'" + MachineImportHelperCommon.HEADER_INSTALLED + "' cannot be specified if '"
                        + MachineImportHelperCommon.HEADER_ASSEMBLY_PART + "' is specified";
                return new ValidInfo(isValid, validString);
            }

            // must have assigned inventory item
            if ((assignedItem == null) || (!(assignedItem instanceof ItemDomainInventory))) {
                isValid = false;
                validString = "'" + MachineImportHelperCommon.HEADER_INSTALLED + "' cannot be specified unless '"
                        + MachineImportHelperCommon.HEADER_ASSIGNED_ITEM + "' specifies an inventory item";
                return new ValidInfo(isValid, validString);
            }
            
        } else {
            
            // must specify isInstalled with assigned inventory item
            if (assignedItem != null && assignedItem instanceof ItemDomainInventory) {
                isValid = false;
                validString = "Must specify '" + MachineImportHelperCommon.HEADER_INSTALLED + "' with assigned inventory item.";
                return new ValidInfo(isValid, validString);
            }
        }

        // handle assigned item
        ItemDomainMachineDesignControllerUtility utility = new ItemDomainMachineDesignControllerUtility();
        try {
            utility.updateAssignedItem(item, assignedItem, user, isInstalled);
        } catch (CdbException ex) {
            isValid = false;
            validString = "Error updating assigned item: " + ex.getMessage();
            return new ValidInfo(isValid, validString);
        }

        return new ValidInfo(isValid, validString);
    }
    
}
