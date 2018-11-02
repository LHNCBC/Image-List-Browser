The Image List Browser uses the text files in this folder to dynamically build the different attributes that image annotations made with ILB can have.

Each file has 4 required lines and then additional optional lines. The first line is a character, the character that marks the attribute type ex: Gender attributes are recorded as "g[Male/Female/...]" the g is the character that the Gender text file has for the first line.

The second line is a short one/two word description of the Attribute, which gets displayed in ILB so the user knows what Attribute they're marking.

The third line is the number of elements that can be selected for the Attribute being defined. A negative value means there is limit. Ex: Gender has 1 since people don't normally have multiple genders. While Occlusions is -1 since you can have multiple occlusions on a face (eg: beard and mustache). 

The fourth line is a comma separated list of the different values the Attribute can be assigned.

The fifth line and beyond are colon separated lists that define unallowed states for the Attribute (eg: unoccluded:glasses) means you can't say the image is unoccluded and still be occluded by glasses. 