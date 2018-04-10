package com.sentaroh.android.SMBSync2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;


public class JcifsFile {

    private boolean mSmb1=true;

    private JcifsAuth mAuth=null;

    private jcifsng.smb.SmbFile mSmb2File =null;
    private jcifs.smb.SmbFile mSmb1File =null;

    public JcifsFile(String url, JcifsAuth auth) throws MalformedURLException {
        mSmb1=auth.isSmb1Auth();
        mAuth=auth;
        
    	if (mSmb1) {
    		mSmb1File =new jcifs.smb.SmbFile(url, auth.getSmb1Auth());
    	} else {
    		mSmb2File =new jcifsng.smb.SmbFile(url,auth.getSmb2Auth());
    	}
    }

    public JcifsFile(jcifs.smb.SmbFile smb1File, JcifsAuth auth) {
        mSmb1=JcifsAuth.JCIFS_FILE_SMB1;
        mAuth=auth;
        mSmb1File =smb1File;
    }

    public JcifsFile(jcifsng.smb.SmbFile smb2File, JcifsAuth auth) {
        mSmb1=JcifsAuth.JCIFS_FILE_SMB2;
        mAuth=auth;
        mSmb2File =smb2File;
    }

    public boolean isSmb1File() {
    	return mSmb1;
    }

    public jcifs.smb.SmbFile getSmb1File() {
        return mSmb1File;
    }

    public jcifsng.smb.SmbFile getSmb2File() {
        return mSmb2File;
    }

    public boolean exists()   throws JcifsException {
        try {
            if (mSmb1) {
        		return mSmb1File.exists();
        	} else {
        		return mSmb2File.exists();
        	}
		} catch (jcifsng.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (jcifs.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		}
    	
    }

    public void delete()   throws JcifsException{
        try {
            if (mSmb1) {
        		mSmb1File.delete();
        	} else {
        		mSmb2File.delete();
        	}
		} catch (jcifsng.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (jcifs.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		}
    	
    }

    public void mkdir()   throws JcifsException {
        try {
            if (mSmb1) {
        		mSmb1File.mkdir();
        	} else {
        		mSmb2File.mkdir();
        	}
		} catch (jcifsng.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (jcifs.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		}
    	
    }

    public void mkdirs()   throws JcifsException{
        try {
        	if (mSmb1) {
        		mSmb1File.mkdirs();
        	} else {
        		mSmb2File.mkdirs();
        	}
		} catch (jcifsng.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (jcifs.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		}
    	
    }

    public int getAttributes() throws JcifsException {
        try {
        	if (mSmb1) {
        		return mSmb1File.getAttributes();
        	} else {
        		return mSmb2File.getAttributes();
        	}
		} catch (jcifsng.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (jcifs.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		}
    }
    
    public InputStream getInputStream()   throws JcifsException {
        try {
        	if (mSmb1) {
        		return mSmb1File.getInputStream();
        	} else {
        		return mSmb2File.getInputStream();
        	}
		} catch (jcifsng.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (jcifs.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (IOException e) {
			throw(new JcifsException(e, 0, e.getCause()));
		}
    	
    }


    public OutputStream getOutputStream()  throws JcifsException{
        try {
        	if (mSmb1) {
        		return mSmb1File.getOutputStream();
        	} else {
        		return mSmb2File.getOutputStream();
        	}
		} catch (jcifsng.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (jcifs.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (IOException e) {
			throw(new JcifsException(e, 0, e.getCause()));
		}
    	
    }
    
    public void connect() throws JcifsException{
        try {
        	if (mSmb1) {
        		mSmb1File.connect();
        	} else {
        		mSmb2File.connect();
        	}
		} catch (jcifsng.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (jcifs.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (IOException e) {
			throw(new JcifsException(e, 0, e.getCause()));
		}

    }

    public void createNew() throws JcifsException {
        try {
        	if (mSmb1) {
        		mSmb1File.createNewFile();
        	} else {
        		mSmb2File.createNewFile();
        	}
		} catch (jcifsng.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (jcifs.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		}
    	
    }
    
    public String getName() {
        if (mSmb1) {
			return mSmb1File.getName();
		} else {
			return mSmb2File.getName();
		}
    	
    }

    public String getPath() {
        if (mSmb1) {
			return mSmb1File.getPath();
		} else {
			return mSmb2File.getPath();
		}
    }

    public String getCanonicalPath() {
        if (mSmb1) {
			return mSmb1File.getCanonicalPath();
		} else {
			return mSmb2File.getCanonicalPath();
		}
    }

    public String getShare() {
        if (mSmb1) {
			return mSmb1File.getShare();
		} else {
			return mSmb2File.getShare();
		}
    }

    public int getType() throws JcifsException {
        try {
            if (mSmb1) {
    			return mSmb1File.getType();
    		} else {
    			return mSmb2File.getType();
    		}
        } catch (jcifsng.smb.SmbException e) {
            throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
        } catch (jcifs.smb.SmbException e) {
            throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public String getUncPath() {
        if (mSmb1) {
			return mSmb1File.getUncPath();
		} else {
			return mSmb2File.getUncPath();
		}
    }

    public String getParent() {
        if (mSmb1) {
			return mSmb1File.getParent();
		} else {
			return mSmb2File.getParent();
		}
    	
    }

    public boolean canRead()   throws JcifsException {
        try {
            if (mSmb1) {
                return mSmb1File.canRead();
            } else {
                return mSmb2File.canRead();
            }
        } catch (jcifsng.smb.SmbException e) {
            throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
        } catch (jcifs.smb.SmbException e) {
            throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public boolean canWrite()   throws JcifsException {
        try {
            if (mSmb1) {
                return mSmb1File.canWrite();
            } else {
                return mSmb2File.canWrite();
            }
        } catch (jcifsng.smb.SmbException e) {
            throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
        } catch (jcifs.smb.SmbException e) {
            throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public boolean isDirectory() throws JcifsException {
        try {
        	if (mSmb1) {
        		return mSmb1File.isDirectory();
        	} else {
        		return mSmb2File.isDirectory();
        	}
		} catch (jcifsng.smb.SmbException e) {
            throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (jcifs.smb.SmbException e) {
            throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		}
    	
    }

    public boolean isFile() throws JcifsException {
        try {
        	if (mSmb1) {
        		return mSmb1File.isFile();
        	} else {
        		return mSmb2File.isFile();
        	}
		} catch (jcifsng.smb.SmbException e) {
            throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (jcifs.smb.SmbException e) {
            throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		}
    	
    }

    public boolean isHidden() throws JcifsException {
        try {
        	if (mSmb1) {
        		return mSmb1File.isHidden();
        	} else {
        		return mSmb2File.isHidden();
        	}
		} catch (jcifsng.smb.SmbException e) {
            throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (jcifs.smb.SmbException e) {
            throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		}
    }
    
    public long length() throws JcifsException {
        try {
        	if (mSmb1) {
        		return mSmb1File.length();
        	} else {
        		return mSmb2File.length();
        	}
		} catch (jcifsng.smb.SmbException e) {
            throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (jcifs.smb.SmbException e) {
            throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		}
    	
    }
    
    public String[] list() throws JcifsException {
        try {
        	if (mSmb1) return mSmb1File.list();
        	else return mSmb2File.list();
		} catch (jcifsng.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (jcifs.smb.SmbException e) {
            throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		}
    	
    }

    public JcifsFile[] listFiles() throws JcifsException {
        try {
        	if (mSmb1) {
        		jcifs.smb.SmbFile[] files= mSmb1File.listFiles();
        		if (files==null) return null;
        		JcifsFile[] result=new JcifsFile[files.length];
        		for(int i=0;i<files.length;i++) result[i]=new JcifsFile(files[i], mAuth);
        		return result;
        	} else {
                jcifsng.smb.SmbFile[] files= mSmb2File.listFiles();
        		if (files==null) return null;
        		JcifsFile[] result=new JcifsFile[files.length];
        		for(int i=0;i<files.length;i++) result[i]=new JcifsFile(files[i], mAuth);
        		return result;
        	}
		} catch (jcifsng.smb.SmbException e) {
            throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (jcifs.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }
    
    public void renameTo ( JcifsFile d ) throws JcifsException {
        try {
        	if (mSmb1) {
//        		jcifs.smb.SmbFile to=new jcifs.smb.SmbFile(d.getPath(), d.getAuth().getSmb1Auth());
                if (d.getSmb1File()==null) throw new JcifsException(new Exception("Null SMB1 authentication"), 0, null);
        		mSmb1File.renameTo(d.getSmb1File());
        	} else {
//                jcifsng.smb.SmbFile to=new jcifsng.smb.SmbFile(d.getPath(), d.getAuth().getSmb2Auth());
                if (d.getSmb2File()==null) throw new JcifsException(new Exception("Null SMB2 authentication"), 0, null);
        		mSmb2File.renameTo(d.getSmb2File());
        	}
		} catch (jcifsng.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (jcifs.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
        }
    }

    public JcifsAuth getAuth() {
    	return mAuth;
    }

    
    public void setLastModified(long lm) throws JcifsException {
        try {
        	if (mSmb1) {
        	    mSmb1File.setLastModified(lm);
            } else {
        	    mSmb2File.setLastModified(lm);
            }
		} catch (jcifsng.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (jcifs.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		}
    }

    public long getLastModified() throws JcifsException {
        try {
			return mSmb1? mSmb1File.lastModified(): mSmb2File.lastModified();
		} catch (jcifsng.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		} catch (jcifs.smb.SmbException e) {
			throw(new JcifsException(e, e.getNtStatus(), e.getCause()));
		}
    }

}