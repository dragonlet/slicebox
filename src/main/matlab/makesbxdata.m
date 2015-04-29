function sbxdata = makesbxdata(username, password, varargin)
% MAKESBXDATA Specify parameters for a SliceBox connection.
%   sbxdata = MAKESBXDATA(username, password) Creates an sbxdata object
%       using username and password to connect to the server. The rest of 
%       the parameters are read from the file 'sbxconf.settings'.
%
%   sbxdata = MAKESBXDATA(username, password, url, cachepath) In case no
%       'sbxconf.settings' file exists, the parameters 'url' and 
%       'cachepath' are required.
%
%   sbxdata = MAKESBXDATA(_, Name, Value) Specify the 'url' or 'cachepath'
%       to overwrite the parameters from 'sbxconf.settings'.

sbxdata = struct;
p = inputParser;
%If settings file exist, use contents as default. Otherwise, require url
%and cachepath.
if (exist('sbxconf.settings', 'file') == 2)
    sbxconf = fopen('sbxconf.settings');
    cells = textscan(sbxconf,'%s %s','delimiter','=');
    fclose(sbxconf);
    names = cells{1};
    values = cells{2};
    conf = cell2struct(values, names);

    addRequired(p, 'username');
    addRequired(p, 'password');
    addParameter(p, 'url', conf.url);
    addParameter(p, 'cachepath', conf.cachepath);
    parse(p,username,password,varargin{:});
else
    p = inputParser;
    addRequired(p, 'username');
    addRequired(p, 'password');
    addRequired(p, 'url');
    addRequired(p, 'cachepath');
    parse(p, username, password, varargin{:});
end

wo = weboptions('Username', p.Results.username, 'Password', p.Results.password');
sbxdata.weboptions = wo;
sbxdata.url = p.Results.url;
sbxdata.cachepath = p.Results.cachepath;

%Test for cache directory
cachepath = sbxdata.cachepath;
if exist(cachepath, 'dir') ~= 7
    ME = MException('SBXMakesbxdata:noCacheDirectory','Cache directory does not exist!');
    throw(ME);
end