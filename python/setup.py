from setuptools import setup
from codecs import open
from os import path

"""
  Refer to http://peterdowns.com/posts/first-time-with-pypi.html for instructions on how to 
  push new versions of this module to PyPI. Use the following command to push new versions of the module:
  
  python3 setup.py sdist upload -r pypi
  
  Make sure you update the MANIFEST.in file if you change the name of the JAR file so its included in the 
  module when submitted
"""

# Get the long description from the README file
here = path.abspath(path.dirname(__file__))
with open(path.join(here, 'README.md'), encoding='utf-8') as f:
    long_description = f.read()

setup(
  name = 'jmxquery',
  packages = ['jmxquery'],
  version = '0.4.0',
  description = 'A JMX Interface for Python to Query runtime metrics in a JVM',
  long_description=long_description,
  author = 'David Gildeh',
  author_email = 'david.gildeh@outlyer.com',
  url = 'https://github.com/outlyerapp/jmxquery',
  keywords = ['java', 'jmx', 'metrics', 'monitoring'],
  classifiers = [],
  include_package_data=True,
  zip_safe = False
)