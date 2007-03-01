#!/bin/sh
###
### Utility script functions common to server startup scripts
###

sipx_config_value() { # ( CONFIG-FILE, DIRECTIVE )
#   prints the value of DIRECTIVE from CONFIG-FILE 
#   list values have any commas removed
    perl -n \
        -e 'use English;' \
        -e 's/#.*$//;' \
        -e "/^\\s*$2\\s*:\\s*/ && print join( ' ', split( /[\\s,]+/, \$POSTMATCH ));" \
        $1
}

dns_sipsrv () { # ( TRANSPORT, DOMAIN )
  # returns the SRV host name for DOMAIN over TRANSPORT 
  target=`echo _sip._$1.$2 | tr A-Z a-z`
  dig  -t srv +noidentify +nocmd +nocomments +noquestion +nostats +noauthority ${target} \
  | tr A-Z a-z \
  | awk "\$1 == \"${target}.\" { print \$NF }" \
  | sed 's/\.$//'
  }

dns_cname () { # ( DOMAIN )
  # returns the CNAME resolution for DOMAIN
  target=`echo $1 | tr A-Z a-z`
  dig  -t cname +noidentify +nocmd +nocomments +noquestion +nostats +noauthority ${target} \
  | tr A-Z a-z \
  | awk "\$1 == \"${target}.\" { print \$NF }" \
  | sed 's/\.$//'
  }

dns_a () { # ( DOMAIN )
  # returns the A record resolution for DOMAIN
  target=`echo $1 | tr A-Z a-z`
  dig  -t a +noidentify +nocmd +nocomments +noquestion +nostats +noauthority ${target} \
  | tr A-Z a-z \
  | awk "\$1 == \"${target}.\" { print \$NF }" \
  | sed 's/\.$//'
  }


sip_resolves_to () { # ( unresolved, targetIp )
    # returns true (0) if the unresolved name resolves to the targetIp address by sip rules
     unresolvedName=$1
     targetAddr=$2

     for ip in `dns_a ${unresolvedName}`
     do
       if [ "${ip}" = "${targetAddr}" ]
       then
           return 0
       fi
     done

     for cName in `dns_cname ${unresolvedName}`
     do
       for ip in `dns_a ${cName}`
       do
         if [ "${ip}" = "${targetAddr}" ]
         then
             return 0
         fi
       done
     done

     for tcpSrv in `dns_sipsrv tcp ${unresolvedName}`
     do
       if [ "${tcpSrv}" = "${targetAddr}" ]
       then
           return 0
       else
           for ip in `dns_a ${tcpSrv}`
           do
             if [ "${ip}" = "${targetAddr}" ]
             then
                 return 0
             fi
           done
       fi
     done

     for udpSrv in `dns_sipsrv udp ${unresolvedName}`
     do
       if [ "${udpSrv}" = "${targetAddr}" ]
       then
           return 0
       else
           for ip in `dns_a ${udpSrv}`
           do
             if [ "${ip}" = "${targetAddr}" ]
             then
                 return 0
             fi
           done
       fi
     done
        
     return 1
}
