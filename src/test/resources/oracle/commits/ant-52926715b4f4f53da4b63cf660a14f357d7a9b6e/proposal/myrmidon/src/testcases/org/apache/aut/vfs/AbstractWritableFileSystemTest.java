/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * File system test that check that a file system can be modified.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public abstract class AbstractWritableFileSystemTest
    extends AbstractFileSystemTest
{
    public AbstractWritableFileSystemTest( String name )
    {
        super( name );
    }

    /**
     * Returns the URI for the area to do tests in.
     */
    protected abstract FileObject getWriteFolder() throws Exception;

    /**
     * Sets up a scratch folder for the test to use.
     */
    protected FileObject createScratchFolder() throws Exception
    {
        FileObject scratchFolder = getWriteFolder();

        // Make sure the test folder is empty
        scratchFolder.delete();
        scratchFolder.create( FileType.FOLDER );

        return scratchFolder;
    }

    /**
     * Tests folder creation.
     */
    public void testFolderCreate() throws Exception
    {
        FileObject scratchFolder = createScratchFolder();

        // Create direct child of the test folder
        FileObject folder = scratchFolder.resolveFile( "dir1" );
        assertTrue( !folder.exists() );
        folder.create( FileType.FOLDER );
        assertTrue( folder.exists() );
        assertEquals( 0, folder.getChildren().length );

        // Create a descendant, where the intermediate folders don't exist
        folder = scratchFolder.resolveFile( "dir2/dir1/dir1" );
        assertTrue( !folder.exists() );
        assertTrue( !folder.getParent().exists() );
        assertTrue( !folder.getParent().getParent().exists() );
        folder.create( FileType.FOLDER );
        assertTrue( folder.exists() );
        assertEquals( 0, folder.getChildren().length );
        assertTrue( folder.getParent().exists() );
        assertTrue( folder.getParent().getParent().exists() );

        // Test creating a folder that already exists
        folder.create( FileType.FOLDER );
    }

    /**
     * Tests file creation
     */
    public void testFileCreate() throws Exception
    {
        FileObject scratchFolder = createScratchFolder();

        // Create direct child of the test folder
        FileObject file = scratchFolder.resolveFile( "file1.txt" );
        assertTrue( !file.exists() );
        file.create( FileType.FILE );
        assertTrue( file.exists() );
        assertEquals( 0, file.getContent().getSize() );

        // Create a descendant, where the intermediate folders don't exist
        file = scratchFolder.resolveFile( "dir1/dir1/file1.txt" );
        assertTrue( !file.exists() );
        assertTrue( !file.getParent().exists() );
        assertTrue( !file.getParent().getParent().exists() );
        file.create( FileType.FILE );
        assertTrue( file.exists() );
        assertEquals( 0, file.getContent().getSize() );
        assertTrue( file.getParent().exists() );
        assertTrue( file.getParent().getParent().exists() );

        // Test creating a file that already exists
        file.create( FileType.FILE );
    }

    /**
     * Tests file/folder creation with mismatched types.
     */
    public void testFileCreateMismatched() throws Exception
    {
        FileObject scratchFolder = createScratchFolder();

        // Create a test file and folder
        FileObject file = scratchFolder.resolveFile( "dir1/file1.txt" );
        file.create( FileType.FILE );
        assertEquals( FileType.FILE, file.getType() );

        FileObject folder = scratchFolder.resolveFile( "dir1/dir2" );
        folder.create( FileType.FOLDER );
        assertEquals( FileType.FOLDER, folder.getType() );

        // Attempt to create a file that already exists as a folder
        try
        {
            folder.create( FileType.FILE );
            assertTrue( false );
        }
        catch( FileSystemException exc )
        {
        }

        // Attempt to create a folder that already exists as a file
        try
        {
            file.create( FileType.FOLDER );
            assertTrue( false );
        }
        catch( FileSystemException exc )
        {
        }

        // Attempt to create a folder as a child of a file
        FileObject folder2 = file.resolveFile( "some-child" );
        try
        {
            folder2.create( FileType.FOLDER );
            assertTrue( false );
        }
        catch( FileSystemException exc )
        {
        }
    }

    /**
     * Tests deletion
     */
    public void testDelete() throws Exception
    {
        // Set-up the test structure
        FileObject folder = createScratchFolder();
        folder.resolveFile( "file1.txt" ).create( FileType.FILE );
        folder.resolveFile( "emptydir" ).create( FileType.FOLDER );
        folder.resolveFile( "dir1/file1.txt" ).create( FileType.FILE );
        folder.resolveFile( "dir1/dir2/file2.txt" ).create( FileType.FILE );

        // Delete a file
        FileObject file = folder.resolveFile( "file1.txt" );
        assertTrue( file.exists() );
        file.delete();
        assertTrue( !file.exists() );

        // Delete an empty folder
        file = folder.resolveFile( "emptydir" );
        assertTrue( file.exists() );
        file.delete();
        assertTrue( !file.exists() );

        // Recursive delete
        file = folder.resolveFile( "dir1" );
        FileObject file2 = file.resolveFile( "dir2/file2.txt" );
        assertTrue( file.exists() );
        assertTrue( file2.exists() );
        file.delete();
        assertTrue( !file.exists() );
        assertTrue( !file2.exists() );

        // Delete a file that does not exist
        file = folder.resolveFile( "some-folder/some-file" );
        assertTrue( !file.exists() );
        file.delete();
        assertTrue( !file.exists() );
    }

    /**
     * Test that children are handled correctly by create and delete.
     */
    public void testListChildren() throws Exception
    {
        FileObject folder = createScratchFolder();
        HashSet names = new HashSet();

        // Make sure the folder is empty
        assertEquals( 0, folder.getChildren().length );

        // Create a child folder
        folder.resolveFile( "dir1" ).create( FileType.FOLDER );
        names.add( "dir1" );
        assertSameFileSet( names, folder.getChildren() );

        // Create a child file
        folder.resolveFile( "file1.html" ).create( FileType.FILE );
        names.add( "file1.html" );
        assertSameFileSet( names, folder.getChildren() );

        // Create a descendent
        folder.resolveFile( "dir2/file1.txt" ).create( FileType.FILE );
        names.add( "dir2" );
        assertSameFileSet( names, folder.getChildren() );

        // Create a child file via an output stream
        OutputStream outstr = folder.resolveFile( "file2.txt" ).getContent().getOutputStream();
        outstr.close();
        names.add( "file2.txt" );
        assertSameFileSet( names, folder.getChildren() );

        // Delete a child folder
        folder.resolveFile( "dir1" ).delete();
        names.remove( "dir1" );
        assertSameFileSet( names, folder.getChildren() );

        // Delete a child file
        folder.resolveFile( "file1.html" ).delete();
        names.remove( "file1.html" );
        assertSameFileSet( names, folder.getChildren() );

        // Recreate the folder
        folder.delete();
        folder.create( FileType.FOLDER );
        assertEquals( 0, folder.getChildren().length );
    }

    /**
     * Ensures the names of a set of files match an expected set.
     */
    private void assertSameFileSet( Set names, FileObject[] files )
    {
        // Make sure the sets are the same length
        assertEquals( names.size(), files.length );

        // Check for unexpected names
        for( int i = 0; i < files.length; i++ )
        {
            FileObject file = files[ i ];
            assertTrue( names.contains( file.getName().getBaseName() ) );
        }
    }
}
