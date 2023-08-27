# Hesperus: A fork of Phosphor


### How does it work?

Hesperus makes a variety of modifications to the vanilla lighting engine in order to improve performance. The key highlights can be found below.

- The code responsible for propagating light changes has been completely rewritten to be far more efficient than the vanilla implementation.
- Light updates are postponed until the regions they modify are queried. This allows lighting updates to be batched together more effectively and reduces the number of duplicated scheduled light updates for a block.
  This significantly reduces the CPU time spent propagating skylight updates.
- Skylight propagation on the vertical axis has been fixed to take into account incoming skylight from neighboring chunks, fixing a variety of lighting issues created during world generation and large operations
  involving large block volumes (such as /fill.)
- Chunk lighting is only performed once all adjacent chunks are loaded so sky and block light propogation is spread into neighbors correctly, preventing various visual errors.
- Through fixing various errors in vanilla's lighting engine implementation, many checks performed when relighting blocks are now skipped, reducing the overhead of lighting updates.

This list is still incomplete and a technical writeup of how Hesperus achieves such significant gains is in the works.

### License

Hesperus is licensed under GNU GPLv3, a free and open-source license. For more information, please see the [license file](https://github.com/jellysquid3/Hesperus-forge/blob/master/LICENSE.txt).
