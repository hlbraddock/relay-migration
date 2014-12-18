package org.cru.migration.thekey;

public class Notes
{
/*


relay accounts:
82900761-8729-66CB-3BFE-DCA7906710DA / TRAVIS.GARRISON@USCM.ORG
A56D871D-C828-4534-B251-B8B2FADBB4BB / TRAVIS@SHARONANDTRAVIS.ORG

thekey accounts:
82900761-8729-66CB-3BFE-DCA7906710DA / travis@sharonandtravis.org
no account for travis.garrison@uscm.org

RIGHT WAY:
email / authoritative common guid / relay / thekey
travis@sharonandtravis.org / A56D871D-C828-4534-B251-B8B2FADBB4BB / A56D871D-C828-4534-B251-B8B2FADBB4BB / 82900761-8729-66CB-3BFE-DCA7906710DA
TRAVIS.GARRISON@USCM.ORG / (new guid) / 82900761-8729-66CB-3BFE-DCA7906710DA / (none)

how do we merge those accounts without having accounts share the same email or have accounts share the same GUID?

if we match on email address we get the following accounts:

email / relay / thekey
travis@sharonandtravis.org / A56D871D-C828-4534-B251-B8B2FADBB4BB / 82900761-8729-66CB-3BFE-DCA7906710DA
TRAVIS.GARRISON@USCM.ORG / 82900761-8729-66CB-3BFE-DCA7906710DA / (none)

using your proposed logic when either account logs in to The Key the same GUID would be released.


if we match on guids (using last login to select conflicting attributes) we would end up having 2 accounts that want the email travis@sharonandtravis.org


The second account would have the authoritative common guid released as the key guid, and any new accounts created after the merge would just end up with a authoritative common guid generated for them.


The only real way to address this is to generate a new guid for the merged accounts

In the scripts current logic if it matched guids that were the same, but emails that were different it would use the most recent email
(excluding the override specified by you and Josh that all US accounts should use Relay attributes).

In the case of Travis Garrison it would result in 2 accounts, which based on your relay usage data he has and has used both of in the past year (one account last used November 2013,
other one sometime this year).

The script wasn’t complete, so it didn’t have that captured beyond the first non-US account group being merged.
There were also several (~10) merged accounts that ended up with the same email using the existing script that I manually addressed.
They had been some automatically created links between 2 identities when the user had never actually used the identity on one system or the other, but created a new account instead.
I removed the automated link to let the other link logic fire instead.


-Daniel


On Oct 9, 2014, at 10:58 AM, Lee Braddock <lee.braddock@cru.org> wrote:

Well, I am referring to the edge case you described (travis garrison). In this case theoretically we could merge all the accounts into one.
But your algorithm produced two accounts (possibly very reasonably so), leaving the differing username accounts as separate. This leads to the question what would you do if you encountered a single match on ssoguid but where usernames did not match? The rule that I was employing was merge into one account, where username with most recent login would be taken.

Is that not the appropriate merge rule?

On Thu, Oct 9, 2014 at 10:22 AM, Daniel Frett <daniel.frett@ccci.org> wrote:
The script was a combination analysis & potentially a model for how to handle merging the accounts. I never did finish the script,
I hadn’t finished merging non US staff, and hadn’t dealt with importing remaining identities that didn’t match. I would guess there would be some conflicting email
 addresses due to linking data or preferring guid match over email match.


On Oct 8, 2014, at 5:29 PM, Lee Braddock <lee.braddock@cru.org> wrote:

Also, normally (or so I thought), accounts matching on ssoguid would be merged, where non matching username would be overwritten by most recent login
(where U.S. staff also take precedence). However, your scripts algorithm creates two separate accounts, retaining the matching ssoguid in the separate place holders (one being relay and the other the key).

Are you proposing that all accounts matching ssoguid but not matching username result in two separate accounts? Or, rather, only in the case where there already also exists a
match on username?

no, look at the script again, line 109 and line 224, "process identities with relay & key accounts that have matching guids (but neither account is linked)”
that will merge accounts that have matching guids.

so, currently the script merges accounts in the following preference:

1. linked identities
2. matching guids
3. matching emails
4. everything else

-Daniel


On Wed, Oct 8, 2014 at 5:10 PM, Lee Braddock <lee.braddock@cru.org> wrote:
Are you running merge scripts for your own analysis purposes or for intended production merge use?

On Wed, Oct 8, 2014 at 11:44 AM, Daniel Frett <daniel.frett@ccci.org> wrote:
On Oct 8, 2014, at 11:35 AM, Lee Braddock <lee.braddock@cru.org> wrote:

> A two way merge requires an arbitrary choice as to which relay account we merge with the key.

This is where identity linking data would come into play, and we should then probably prefer email address over guid.

Using account linking data we may end up with 2 accounts wanting the same email address, which I haven’t worked out a solution for yet (I also haven’t
determined how many accounts would actually be affected by this).

-Daniel





 */
}
