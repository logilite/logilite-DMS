### Plugin Purpose

The purpose of this plugin is to enable the following functionalities:

1. **Multiple Uploads:** Users can upload multiple files associated with a specific table and record ID within the application.
2. **Dynamic Directory Saving:** The plugin allows users to save the uploaded files into a directory, providing organization and accessibility to the uploaded documents.

This functionality streamlines the process of managing and accessing multiple files within the application, enhancing usability and efficiency for users.

### Update Content Created Date Based on File Metadata

This plugin provides the functionality to update the DMS content created date based on the file metadata. However, please note that this feature is specifically designed to work with PDF files only.

When a PDF file is uploaded using, the document created date will be automatically extracted from the file metadata and used to update the DMS content, Association and Version created date within the application.

### Note

- This feature is tailored to PDF files and may not function as expected with other file formats.

### Accessing System Configurator to Configure Maximum Upload Size

To set the maximum upload size for document uploads, you'll need to access the System Configurator window. Follow these steps:

1. Log in to your application as the system user. This typically requires administrative privileges.
2. Search System Configurator window in menu. This window is where system-wide settings are managed.

### Updating Maximum Upload Size in System Configurator

Once in the System Configurator window, locate the Configurator named `DMS_ZK_MAX_UPLOAD_SIZE`. This Configurator controls the maximum allowed size, in kilobytes, for uploading files from the client.

1. Look for the `DMS_ZK_MAX_UPLOAD_SIZE` in System Configurator window.
2. Enter the desired value for the maximum upload size in kilobytes. A zero or negative value indicates no limit.
3. Save your changes to apply the new maximum upload size configuration.
