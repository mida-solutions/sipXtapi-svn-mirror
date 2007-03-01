# this is used by sipxbuild to compute a correct build order

.PHONY: sipXbuild
sipXbuild :
	@echo sipXbuild

.PHONY: sipXportLib
sipXportLib :
	@echo sipXportLib

.PHONY: sipXtackLib
sipXtackLib : sipXportLib
	@echo sipXtackLib

.PHONY: sipXmediaLib
sipXmediaLib : sipXtackLib
	@echo sipXmediaLib

.PHONY: sipXmediaAdapterLib
sipXmediaAdapterLib : sipXmediaLib
	@echo sipXmediaAdapterLib

.PHONY: sipXcallLib
sipXcallLib : sipXmediaAdapterLib
	@echo sipXcallLib

.PHONY: sipXcommserverLib
sipXcommserverLib : sipXtackLib
	@echo sipXcommserverLib

.PHONY: sipXpublisher
sipXpublisher : sipXcommserverLib
	@echo sipXpublisher

.PHONY: sipXregistry
sipXregistry : sipXcommserverLib
	@echo sipXregistry

.PHONY: sipXproxy
sipXproxy : sipXcommserverLib
	@echo sipXproxy

.PHONY: sipXconfig
sipXconfig : sipXtackLib
	@echo sipXconfig

.PHONY: sipXvxml
sipXvxml : sipXcallLib sipXcommserverLib sipXmediaAdapterLib
	@echo sipXvxml

.PHONY: sipXpbx
sipXpbx : sipXproxy sipXregistry sipXpublisher sipXvxml sipXconfig
	@echo sipXpbx

.PHONY: bbridge
bbridge : resiprocateLib sipXmediaAdapterLib
	@echo bbridge

.PHONY: resiprocateLib
resiprocateLib : 
	@echo resiprocateLib

